/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.orc.impl;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.orc.CompressionKind;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.orc.OrcFile;
import org.apache.orc.util.BloomFilter;
import org.apache.orc.util.BloomFilterIO;
import org.apache.orc.BooleanColumnStatistics;
import org.apache.orc.ColumnStatistics;
import org.apache.orc.CompressionCodec;
import org.apache.orc.DataReader;
import org.apache.orc.DateColumnStatistics;
import org.apache.orc.DecimalColumnStatistics;
import org.apache.orc.DoubleColumnStatistics;
import org.apache.orc.IntegerColumnStatistics;
import org.apache.orc.OrcConf;
import org.apache.orc.OrcProto;
import org.apache.orc.Reader;
import org.apache.orc.RecordReader;
import org.apache.orc.StringColumnStatistics;
import org.apache.orc.StripeInformation;
import org.apache.orc.TimestampColumnStatistics;
import org.apache.orc.TypeDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.common.io.DiskRange;
import org.apache.hadoop.hive.common.io.DiskRangeList;
import org.apache.hadoop.hive.common.io.DiskRangeList.CreateHelper;
import org.apache.hadoop.hive.common.type.HiveDecimal;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.hadoop.hive.ql.io.sarg.PredicateLeaf;
import org.apache.hadoop.hive.ql.io.sarg.SearchArgument;
import org.apache.hadoop.hive.ql.io.sarg.SearchArgument.TruthValue;
import org.apache.hadoop.hive.serde2.io.DateWritable;
import org.apache.hadoop.hive.serde2.io.HiveDecimalWritable;
import org.apache.hadoop.hive.ql.util.TimestampUtils;
import org.apache.hadoop.io.Text;

public class RecordReaderImpl implements RecordReader {
  static final Logger LOG = LoggerFactory.getLogger(RecordReaderImpl.class);
  private static final boolean isLogDebugEnabled = LOG.isDebugEnabled();
  private static final Object UNKNOWN_VALUE = new Object();
  protected final Path path;
  private final long firstRow;
  private final List<StripeInformation> stripes =
      new ArrayList<StripeInformation>();
  private OrcProto.StripeFooter stripeFooter;
  private final long totalRowCount;
  protected final TypeDescription schema;
  private final List<OrcProto.Type> types;
  private final int bufferSize;
  private final SchemaEvolution evolution;
  // the file included columns indexed by the file's column ids.
  private final boolean[] fileIncluded;
  private final long rowIndexStride;
  private long rowInStripe = 0;
  private int currentStripe = -1;
  private long rowBaseInStripe = 0;
  private long rowCountInStripe = 0;
  private final Map<StreamName, InStream> streams =
      new HashMap<StreamName, InStream>();
  DiskRangeList bufferChunks = null;
  private final TreeReaderFactory.TreeReader reader;
  private final OrcProto.RowIndex[] indexes;
  private final OrcProto.BloomFilterIndex[] bloomFilterIndices;
  private final OrcProto.Stream.Kind[] bloomFilterKind;
  private final SargApplier sargApp;
  // an array about which row groups aren't skipped
  private boolean[] includedRowGroups = null;
  private final DataReader dataReader;
  private final boolean ignoreNonUtf8BloomFilter;
  private final OrcFile.WriterVersion writerVersion;
  private final int maxDiskRangeChunkLimit;

  /**
   * Given a list of column names, find the given column and return the index.
   *
   * @param evolution the mapping from reader to file schema
   * @param columnName  the fully qualified column name to look for
   * @return the file column number or -1 if the column wasn't found
   */
  static int findColumns(SchemaEvolution evolution,
                         String columnName) {
    try {
      return evolution.getFileSchema().findSubtype(columnName).getId();
    } catch (IllegalArgumentException e) {
      return -1;
    }
  }

  /**
   * Find the mapping from predicate leaves to columns.
   * @param sargLeaves the search argument that we need to map
   * @param evolution the mapping from reader to file schema
   * @return an array mapping the sarg leaves to concrete column numbers in the
   * file
   */
  public static int[] mapSargColumnsToOrcInternalColIdx(
                            List<PredicateLeaf> sargLeaves,
                            SchemaEvolution evolution) {
    int[] result = new int[sargLeaves.size()];
    Arrays.fill(result, -1);
    for(int i=0; i < result.length; ++i) {
      String colName = sargLeaves.get(i).getColumnName();
      result[i] = findColumns(evolution, colName);
    }
    return result;
  }

  /**
   * Given a list of column names, find the given column and return the index.
   *
   * @param columnNames the list of potential column names
   * @param columnName  the column name to look for
   * @param rootColumn  offset the result with the rootColumn
   * @return the column number or -1 if the column wasn't found
   */
  private static int findColumns(String[] columnNames,
                         String columnName,
                         int rootColumn) {
    for(int i=0; i < columnNames.length; ++i) {
      if (columnName.equals(columnNames[i])) {
        return i + rootColumn;
      }
    }
    return -1;
  }

  /**
   * Find the mapping from predicate leaves to columns.
   * @param sargLeaves the search argument that we need to map
   * @param columnNames the names of the columns
   * @param rootColumn the offset of the top level row, which offsets the
   *                   result
   * @return an array mapping the sarg leaves to concrete column numbers
   * @deprecated Use #mapSargColumnsToOrcInternalColIdx(List, SchemaEvolution)
   */
  @Deprecated
  public static int[] mapSargColumnsToOrcInternalColIdx(List<PredicateLeaf> sargLeaves,
                                                        String[] columnNames,
                                                        int rootColumn) {
    int[] result = new int[sargLeaves.size()];
    Arrays.fill(result, -1);
    for(int i=0; i < result.length; ++i) {
      String colName = sargLeaves.get(i).getColumnName();
      result[i] = findColumns(columnNames, colName, rootColumn);
    }
    return result;
  }

  protected RecordReaderImpl(ReaderImpl fileReader,
                             Reader.Options options) throws IOException {
    this.writerVersion = fileReader.getWriterVersion();
    if (options.getSchema() == null) {
      if (LOG.isInfoEnabled()) {
        LOG.info("Reader schema not provided -- using file schema " +
            fileReader.getSchema());
      }
      evolution = new SchemaEvolution(fileReader.getSchema(), null, options);
    } else {

      // Now that we are creating a record reader for a file, validate that
      // the schema to read is compatible with the file schema.
      //
      evolution = new SchemaEvolution(fileReader.getSchema(),
                                      options.getSchema(),
                                      options);
      if (LOG.isDebugEnabled() && evolution.hasConversion()) {
        LOG.debug("ORC file " + fileReader.path.toString() +
            " has data type conversion --\n" +
            "reader schema: " + options.getSchema().toString() + "\n" +
            "file schema:   " + fileReader.getSchema());
      }
    }
    this.schema = evolution.getReaderSchema();
    this.path = fileReader.path;
    this.types = fileReader.types;
    this.bufferSize = fileReader.bufferSize;
    this.rowIndexStride = fileReader.rowIndexStride;
    this.ignoreNonUtf8BloomFilter =
        OrcConf.IGNORE_NON_UTF8_BLOOM_FILTERS.getBoolean(fileReader.conf);
    SearchArgument sarg = options.getSearchArgument();
    if (sarg != null && rowIndexStride != 0) {
      sargApp = new SargApplier(sarg,
          rowIndexStride,
          evolution,
          writerVersion,
          fileReader.useUTCTimestamp,
          fileReader.writerUsedProlepticGregorian(),
          fileReader.options.getConvertToProlepticGregorian());
    } else {
      sargApp = null;
    }

    long rows = 0;
    long skippedRows = 0;
    long offset = options.getOffset();
    long maxOffset = options.getMaxOffset();
    for(StripeInformation stripe: fileReader.getStripes()) {
      long stripeStart = stripe.getOffset();
      if (offset > stripeStart) {
        skippedRows += stripe.getNumberOfRows();
      } else if (stripeStart < maxOffset) {
        this.stripes.add(stripe);
        rows += stripe.getNumberOfRows();
      }
    }
    this.maxDiskRangeChunkLimit = OrcConf.ORC_MAX_DISK_RANGE_CHUNK_LIMIT.getInt(fileReader.conf);
    Boolean zeroCopy = options.getUseZeroCopy();
    if (zeroCopy == null) {
      zeroCopy = OrcConf.USE_ZEROCOPY.getBoolean(fileReader.conf);
    }
    if (options.getDataReader() != null) {
      this.dataReader = options.getDataReader().clone();
    } else {
      DataReaderProperties.Builder builder =
          DataReaderProperties.builder()
              .withBufferSize(bufferSize)
              .withCompression(fileReader.compressionKind)
              .withFileSystemSupplier(fileReader.getFileSystemSupplier())
              .withPath(fileReader.path)
              .withTypeCount(types.size())
              .withMaxDiskRangeChunkLimit(maxDiskRangeChunkLimit)
              .withZeroCopy(zeroCopy);
      FSDataInputStream file = fileReader.takeFile();
      if (file != null) {
        builder.withFile(file);
      }
      this.dataReader = RecordReaderUtils.createDefaultDataReader(
          builder.build());
    }
    firstRow = skippedRows;
    totalRowCount = rows;
    Boolean skipCorrupt = options.getSkipCorruptRecords();
    if (skipCorrupt == null) {
      skipCorrupt = OrcConf.SKIP_CORRUPT_DATA.getBoolean(fileReader.conf);
    }

    TreeReaderFactory.ReaderContext readerContext =
        new TreeReaderFactory.ReaderContext()
          .setSchemaEvolution(evolution)
          .skipCorrupt(skipCorrupt)
          .fileFormat(fileReader.getFileVersion())
          .useUTCTimestamp(fileReader.useUTCTimestamp)
          .setProlepticGregorian(fileReader.writerUsedProlepticGregorian(),
                                 fileReader.options.getConvertToProlepticGregorian());
    reader = TreeReaderFactory.createTreeReader(evolution.getReaderSchema(),
        readerContext);

    this.fileIncluded = evolution.getFileIncluded();
    indexes = new OrcProto.RowIndex[types.size()];
    bloomFilterIndices = new OrcProto.BloomFilterIndex[types.size()];
    bloomFilterKind = new OrcProto.Stream.Kind[types.size()];

    try {
      advanceToNextRow(reader, 0L, true);
    } catch (IOException e) {
      // Try to close since this happens in constructor.
      close();
      throw e;
    }
  }

  public static final class PositionProviderImpl implements PositionProvider {
    private final OrcProto.RowIndexEntry entry;
    private int index;

    public PositionProviderImpl(OrcProto.RowIndexEntry entry) {
      this(entry, 0);
    }

    public PositionProviderImpl(OrcProto.RowIndexEntry entry, int startPos) {
      this.entry = entry;
      this.index = startPos;
    }

    @Override
    public long getNext() {
      return entry.getPositions(index++);
    }
  }

  public static final class ZeroPositionProvider implements PositionProvider {
    @Override
    public long getNext() {
      return 0;
    }
  }

  public OrcProto.StripeFooter readStripeFooter(StripeInformation stripe
                                                ) throws IOException {
    return dataReader.readStripeFooter(stripe);
  }

  enum Location {
    BEFORE, MIN, MIDDLE, MAX, AFTER
  }

  /**
   * Given a point and min and max, determine if the point is before, at the
   * min, in the middle, at the max, or after the range.
   * @param point the point to test
   * @param min the minimum point
   * @param max the maximum point
   * @param <T> the type of the comparision
   * @return the location of the point
   */
  static <T> Location compareToRange(Comparable<T> point, T min, T max) {
    int minCompare = point.compareTo(min);
    if (minCompare < 0) {
      return Location.BEFORE;
    } else if (minCompare == 0) {
      return Location.MIN;
    }
    int maxCompare = point.compareTo(max);
    if (maxCompare > 0) {
      return Location.AFTER;
    } else if (maxCompare == 0) {
      return Location.MAX;
    }
    return Location.MIDDLE;
  }

  /**
   * Get the maximum value out of an index entry.
   * @param index
   *          the index entry
   * @return the object for the maximum value or null if there isn't one
   */
  static Object getMax(ColumnStatistics index) {
    return getMax(index, false);
  }

  /**
   * Get the maximum value out of an index entry.
   * Includes option to specify if timestamp column stats values
   * should be in UTC.
   * @param index
   *          the index entry
   * @param useUTCTimestamp
   * @return the object for the maximum value or null if there isn't one
   */
  static Object getMax(ColumnStatistics index, boolean useUTCTimestamp) {
    if (index instanceof IntegerColumnStatistics) {
      return ((IntegerColumnStatistics) index).getMaximum();
    } else if (index instanceof DoubleColumnStatistics) {
      return ((DoubleColumnStatistics) index).getMaximum();
    } else if (index instanceof StringColumnStatistics) {
      return ((StringColumnStatistics) index).getMaximum();
    } else if (index instanceof DateColumnStatistics) {
      return ((DateColumnStatistics) index).getMaximum();
    } else if (index instanceof DecimalColumnStatistics) {
      return ((DecimalColumnStatistics) index).getMaximum();
    } else if (index instanceof TimestampColumnStatistics) {
      if (useUTCTimestamp) {
        return ((TimestampColumnStatistics) index).getMaximumUTC();
      } else {
        return ((TimestampColumnStatistics) index).getMaximum();
      }
    } else if (index instanceof BooleanColumnStatistics) {
      if (((BooleanColumnStatistics)index).getTrueCount()!=0) {
        return Boolean.TRUE;
      } else {
        return Boolean.FALSE;
      }
    } else {
      return null;
    }
  }

  /**
   * Get the minimum value out of an index entry.
   * @param index
   *          the index entry
   * @return the object for the minimum value or null if there isn't one
   */
  static Object getMin(ColumnStatistics index) {
    return getMin(index, false);
  }

  /**
   * Get the minimum value out of an index entry.
   * Includes option to specify if timestamp column stats values
   * should be in UTC.
   * @param index
   *          the index entry
   * @param useUTCTimestamp
   * @return the object for the minimum value or null if there isn't one
   */
  static Object getMin(ColumnStatistics index, boolean useUTCTimestamp) {
    if (index.getNumberOfValues() == 0) {
      return null;
    } else if (index instanceof IntegerColumnStatistics) {
      return ((IntegerColumnStatistics) index).getMinimum();
    } else if (index instanceof DoubleColumnStatistics) {
      return ((DoubleColumnStatistics) index).getMinimum();
    } else if (index instanceof StringColumnStatistics) {
      return ((StringColumnStatistics) index).getMinimum();
    } else if (index instanceof DateColumnStatistics) {
      return ((DateColumnStatistics) index).getMinimum();
    } else if (index instanceof DecimalColumnStatistics) {
      return ((DecimalColumnStatistics) index).getMinimum();
    } else if (index instanceof TimestampColumnStatistics) {
      if (useUTCTimestamp) {
        return ((TimestampColumnStatistics) index).getMinimumUTC();
      } else {
        return ((TimestampColumnStatistics) index).getMinimum();
      }
    } else if (index instanceof BooleanColumnStatistics) {
      if (((BooleanColumnStatistics)index).getFalseCount()!=0) {
        return Boolean.FALSE;
      } else {
        return Boolean.TRUE;
      }
    } else {
      return UNKNOWN_VALUE; // null is not safe here
    }
  }

  /**
   * Evaluate a predicate with respect to the statistics from the column
   * that is referenced in the predicate.
   * @param statsProto the statistics for the column mentioned in the predicate
   * @param predicate the leaf predicate we need to evaluation
   * @param bloomFilter the bloom filter
   * @param writerVersion the version of software that wrote the file
   * @param type what is the kind of this column
   * @return the set of truth values that may be returned for the given
   *   predicate.
   */
  static TruthValue evaluatePredicateProto(OrcProto.ColumnStatistics statsProto,
                                           PredicateLeaf predicate,
                                           OrcProto.Stream.Kind kind,
                                           OrcProto.ColumnEncoding encoding,
                                           OrcProto.BloomFilter bloomFilter,
                                           OrcFile.WriterVersion writerVersion,
                                           TypeDescription type) {
    return evaluatePredicateProto(statsProto, predicate, kind, encoding, bloomFilter,
        writerVersion, type, false, false, false);
  }

  /**
   * Evaluate a predicate with respect to the statistics from the column
   * that is referenced in the predicate.
   * Includes option to specify if timestamp column stats values
   * should be in UTC.
   * @param statsProto the statistics for the column mentioned in the predicate
   * @param predicate the leaf predicate we need to evaluation
   * @param bloomFilter the bloom filter
   * @param writerVersion the version of software that wrote the file
   * @param type what is the kind of this column
   * @param useUTCTimestamp
   * @return the set of truth values that may be returned for the given
   *   predicate.
   */
  static TruthValue evaluatePredicateProto(OrcProto.ColumnStatistics statsProto,
                                           PredicateLeaf predicate,
                                           OrcProto.Stream.Kind kind,
                                           OrcProto.ColumnEncoding encoding,
                                           OrcProto.BloomFilter bloomFilter,
                                           OrcFile.WriterVersion writerVersion,
                                           TypeDescription type,
                                           boolean useUTCTimestamp,
                                           boolean writerUsedProlepticGregorian,
                                           boolean convertToProlepticGregorian) {
    ColumnStatistics cs = ColumnStatisticsImpl.deserialize(
        null, statsProto, writerUsedProlepticGregorian, convertToProlepticGregorian);
    Object minValue = getMin(cs, useUTCTimestamp);
    Object maxValue = getMax(cs, useUTCTimestamp);
    // files written before ORC-135 stores timestamp wrt to local timezone causing issues with PPD.
    // disable PPD for timestamp for all old files
    TypeDescription.Category category = type.getCategory();
    if (category == TypeDescription.Category.TIMESTAMP) {
      if (!writerVersion.includes(OrcFile.WriterVersion.ORC_135)) {
        LOG.debug("Not using predication pushdown on {} because it doesn't " +
                  "include ORC-135. Writer version: {}",
            predicate.getColumnName(), writerVersion);
        return TruthValue.YES_NO_NULL;
      }
      if (predicate.getType() != PredicateLeaf.Type.TIMESTAMP &&
          predicate.getType() != PredicateLeaf.Type.DATE &&
          predicate.getType() != PredicateLeaf.Type.STRING) {
        return TruthValue.YES_NO_NULL;
      }
    } else if (writerVersion == OrcFile.WriterVersion.ORC_135 &&
               category == TypeDescription.Category.DECIMAL &&
               type.getPrecision() <= TypeDescription.MAX_DECIMAL64_PRECISION) {
      // ORC 1.5.0 to 1.5.5, which use WriterVersion.ORC_135, have broken
      // min and max values for decimal64. See ORC-517.
      LOG.debug("Not using predicate push down on {}, because the file doesn't"+
                   " include ORC-517. Writer version: {}",
          predicate.getColumnName(), writerVersion);
      return TruthValue.YES_NO_NULL;
    }
    return evaluatePredicateRange(predicate, minValue, maxValue, cs.hasNull(),
        BloomFilterIO.deserialize(kind, encoding, writerVersion, type.getCategory(),
            bloomFilter), useUTCTimestamp);
  }

  /**
   * Evaluate a predicate with respect to the statistics from the column
   * that is referenced in the predicate.
   * @param stats the statistics for the column mentioned in the predicate
   * @param predicate the leaf predicate we need to evaluation
   * @return the set of truth values that may be returned for the given
   *   predicate.
   */
  public static TruthValue evaluatePredicate(ColumnStatistics stats,
                                             PredicateLeaf predicate,
                                             BloomFilter bloomFilter) {
    return evaluatePredicate(stats, predicate, bloomFilter, false);
  }

  /**
   * Evaluate a predicate with respect to the statistics from the column
   * that is referenced in the predicate.
   * Includes option to specify if timestamp column stats values
   * should be in UTC.
   * @param stats the statistics for the column mentioned in the predicate
   * @param predicate the leaf predicate we need to evaluation
   * @param bloomFilter
   * @param useUTCTimestamp
   * @return the set of truth values that may be returned for the given
   *   predicate.
   */
  public static TruthValue evaluatePredicate(ColumnStatistics stats,
                                             PredicateLeaf predicate,
                                             BloomFilter bloomFilter,
                                             boolean useUTCTimestamp) {
    Object minValue = getMin(stats, useUTCTimestamp);
    Object maxValue = getMax(stats, useUTCTimestamp);
    return evaluatePredicateRange(predicate, minValue, maxValue, stats.hasNull(), bloomFilter, useUTCTimestamp);
  }

  static TruthValue evaluatePredicateRange(PredicateLeaf predicate, Object min,
      Object max, boolean hasNull, BloomFilter bloomFilter, boolean useUTCTimestamp) {
    // if we didn't have any values, everything must have been null
    if (min == null) {
      if (predicate.getOperator() == PredicateLeaf.Operator.IS_NULL) {
        return TruthValue.YES;
      } else {
        return TruthValue.NULL;
      }
    } else if (min == UNKNOWN_VALUE) {
      return TruthValue.YES_NO_NULL;
    }

    TruthValue result;
    Object baseObj = predicate.getLiteral();
    // Predicate object and stats objects are converted to the type of the predicate object.
    Object minValue = getBaseObjectForComparison(predicate.getType(), min);
    Object maxValue = getBaseObjectForComparison(predicate.getType(), max);
    Object predObj = getBaseObjectForComparison(predicate.getType(), baseObj);

    result = evaluatePredicateMinMax(predicate, predObj, minValue, maxValue, hasNull);
    if (shouldEvaluateBloomFilter(predicate, result, bloomFilter)) {
      return evaluatePredicateBloomFilter(predicate, predObj, bloomFilter, hasNull, useUTCTimestamp);
    } else {
      return result;
    }
  }

  private static boolean shouldEvaluateBloomFilter(PredicateLeaf predicate,
      TruthValue result, BloomFilter bloomFilter) {
    // evaluate bloom filter only when
    // 1) Bloom filter is available
    // 2) Min/Max evaluation yield YES or MAYBE
    // 3) Predicate is EQUALS or IN list
    if (bloomFilter != null
        && result != TruthValue.NO_NULL && result != TruthValue.NO
        && (predicate.getOperator().equals(PredicateLeaf.Operator.EQUALS)
            || predicate.getOperator().equals(PredicateLeaf.Operator.NULL_SAFE_EQUALS)
            || predicate.getOperator().equals(PredicateLeaf.Operator.IN))) {
      return true;
    }
    return false;
  }

  private static TruthValue evaluatePredicateMinMax(PredicateLeaf predicate, Object predObj,
      Object minValue,
      Object maxValue,
      boolean hasNull) {
    Location loc;

    switch (predicate.getOperator()) {
      case NULL_SAFE_EQUALS:
        loc = compareToRange((Comparable) predObj, minValue, maxValue);
        if (loc == Location.BEFORE || loc == Location.AFTER) {
          return TruthValue.NO;
        } else {
          return TruthValue.YES_NO;
        }
      case EQUALS:
        loc = compareToRange((Comparable) predObj, minValue, maxValue);
        if (minValue.equals(maxValue) && loc == Location.MIN) {
          return hasNull ? TruthValue.YES_NULL : TruthValue.YES;
        } else if (loc == Location.BEFORE || loc == Location.AFTER) {
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        } else {
          return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
        }
      case LESS_THAN:
        loc = compareToRange((Comparable) predObj, minValue, maxValue);
        if (loc == Location.AFTER) {
          return hasNull ? TruthValue.YES_NULL : TruthValue.YES;
        } else if (loc == Location.BEFORE || loc == Location.MIN) {
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        } else {
          return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
        }
      case LESS_THAN_EQUALS:
        loc = compareToRange((Comparable) predObj, minValue, maxValue);
        if (loc == Location.AFTER || loc == Location.MAX) {
          return hasNull ? TruthValue.YES_NULL : TruthValue.YES;
        } else if (loc == Location.BEFORE) {
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        } else {
          return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
        }
      case IN:
        if (minValue.equals(maxValue)) {
          // for a single value, look through to see if that value is in the
          // set
          for (Object arg : predicate.getLiteralList()) {
            predObj = getBaseObjectForComparison(predicate.getType(), arg);
            loc = compareToRange((Comparable) predObj, minValue, maxValue);
            if (loc == Location.MIN) {
              return hasNull ? TruthValue.YES_NULL : TruthValue.YES;
            }
          }
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        } else {
          // are all of the values outside of the range?
          for (Object arg : predicate.getLiteralList()) {
            predObj = getBaseObjectForComparison(predicate.getType(), arg);
            loc = compareToRange((Comparable) predObj, minValue, maxValue);
            if (loc == Location.MIN || loc == Location.MIDDLE ||
                loc == Location.MAX) {
              return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
            }
          }
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        }
      case BETWEEN:
        List<Object> args = predicate.getLiteralList();
        if (args == null || args.isEmpty()) {
          return TruthValue.YES_NO;
        }
        Object predObj1 = getBaseObjectForComparison(predicate.getType(), args.get(0));

        loc = compareToRange((Comparable) predObj1, minValue, maxValue);
        if (loc == Location.BEFORE || loc == Location.MIN) {
          Object predObj2 = getBaseObjectForComparison(predicate.getType(), args.get(1));
          Location loc2 = compareToRange((Comparable) predObj2, minValue, maxValue);
          if (loc2 == Location.AFTER || loc2 == Location.MAX) {
            return hasNull ? TruthValue.YES_NULL : TruthValue.YES;
          } else if (loc2 == Location.BEFORE) {
            return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
          } else {
            return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
          }
        } else if (loc == Location.AFTER) {
          return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
        } else {
          return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
        }
      case IS_NULL:
        // min = null condition above handles the all-nulls YES case
        return hasNull ? TruthValue.YES_NO : TruthValue.NO;
      default:
        return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
    }
  }

  private static TruthValue evaluatePredicateBloomFilter(PredicateLeaf predicate,
      final Object predObj, BloomFilter bloomFilter, boolean hasNull, boolean useUTCTimestamp) {
    switch (predicate.getOperator()) {
      case NULL_SAFE_EQUALS:
        // null safe equals does not return *_NULL variant. So set hasNull to false
        return checkInBloomFilter(bloomFilter, predObj, false, useUTCTimestamp);
      case EQUALS:
        return checkInBloomFilter(bloomFilter, predObj, hasNull, useUTCTimestamp);
      case IN:
        for (Object arg : predicate.getLiteralList()) {
          // if atleast one value in IN list exist in bloom filter, qualify the row group/stripe
          Object predObjItem = getBaseObjectForComparison(predicate.getType(), arg);
          TruthValue result = checkInBloomFilter(bloomFilter, predObjItem, hasNull, useUTCTimestamp);
          if (result == TruthValue.YES_NO_NULL || result == TruthValue.YES_NO) {
            return result;
          }
        }
        return hasNull ? TruthValue.NO_NULL : TruthValue.NO;
      default:
        return hasNull ? TruthValue.YES_NO_NULL : TruthValue.YES_NO;
    }
  }

  private static TruthValue checkInBloomFilter(BloomFilter bf, Object predObj, boolean hasNull, boolean useUTCTimestamp) {
    TruthValue result = hasNull ? TruthValue.NO_NULL : TruthValue.NO;

    if (predObj instanceof Long) {
      if (bf.testLong(((Long) predObj).longValue())) {
        result = TruthValue.YES_NO_NULL;
      }
    } else if (predObj instanceof Double) {
      if (bf.testDouble(((Double) predObj).doubleValue())) {
        result = TruthValue.YES_NO_NULL;
      }
    } else if (predObj instanceof String || predObj instanceof Text ||
        predObj instanceof HiveDecimalWritable ||
        predObj instanceof BigDecimal) {
      if (bf.testString(predObj.toString())) {
        result = TruthValue.YES_NO_NULL;
      }
    } else if (predObj instanceof Timestamp) {
      if (useUTCTimestamp) {
        if (bf.testLong(((Timestamp) predObj).getTime())) {
          result = TruthValue.YES_NO_NULL;
        }
      } else {
        if (bf.testLong(SerializationUtils.convertToUtc(TimeZone.getDefault(), ((Timestamp) predObj).getTime()))) {
          result = TruthValue.YES_NO_NULL;
        }
      }
    } else if (predObj instanceof Date) {
      if (bf.testLong(DateWritable.dateToDays((Date) predObj))) {
        result = TruthValue.YES_NO_NULL;
      }
    } else {
        // if the predicate object is null and if hasNull says there are no nulls then return NO
        if (predObj == null && !hasNull) {
          result = TruthValue.NO;
        } else {
          result = TruthValue.YES_NO_NULL;
        }
      }

    if (result == TruthValue.YES_NO_NULL && !hasNull) {
      result = TruthValue.YES_NO;
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Bloom filter evaluation: " + result.toString());
    }

    return result;
  }

  /**
   * An exception for when we can't cast things appropriately
   */
  static class SargCastException extends IllegalArgumentException {

    public SargCastException(String string) {
      super(string);
    }
  }

  private static Object getBaseObjectForComparison(PredicateLeaf.Type type, Object obj) {
    if (obj == null) {
      return null;
    }
    switch (type) {
      case BOOLEAN:
        if (obj instanceof Boolean) {
          return obj;
        } else {
          // will only be true if the string conversion yields "true", all other values are
          // considered false
          return Boolean.valueOf(obj.toString());
        }
      case DATE:
        if (obj instanceof Date) {
          return obj;
        } else if (obj instanceof String) {
          return Date.valueOf((String) obj);
        } else if (obj instanceof Timestamp) {
          return DateWritable.timeToDate(((Timestamp) obj).getTime() / 1000L);
        }
        // always string, but prevent the comparison to numbers (are they days/seconds/milliseconds?)
        break;
      case DECIMAL:
        if (obj instanceof Boolean) {
          return new HiveDecimalWritable(((Boolean) obj).booleanValue() ?
              HiveDecimal.ONE : HiveDecimal.ZERO);
        } else if (obj instanceof Integer) {
          return new HiveDecimalWritable(((Integer) obj).intValue());
        } else if (obj instanceof Long) {
          return new HiveDecimalWritable(((Long) obj));
        } else if (obj instanceof Float || obj instanceof Double ||
            obj instanceof String) {
          return new HiveDecimalWritable(obj.toString());
        } else if (obj instanceof BigDecimal) {
          return new HiveDecimalWritable(HiveDecimal.create((BigDecimal) obj));
        } else if (obj instanceof HiveDecimal) {
          return new HiveDecimalWritable((HiveDecimal) obj);
        } else if (obj instanceof HiveDecimalWritable) {
          return obj;
        } else if (obj instanceof Timestamp) {
          return new HiveDecimalWritable(Double.toString(
              TimestampUtils.getDouble((Timestamp) obj)));
        }
        break;
      case FLOAT:
        if (obj instanceof Number) {
          // widening conversion
          return ((Number) obj).doubleValue();
        } else if (obj instanceof HiveDecimal) {
          return ((HiveDecimal) obj).doubleValue();
        } else if (obj instanceof String) {
          return Double.valueOf(obj.toString());
        } else if (obj instanceof Timestamp) {
          return TimestampUtils.getDouble((Timestamp) obj);
        } else if (obj instanceof HiveDecimal) {
          return ((HiveDecimal) obj).doubleValue();
        } else if (obj instanceof BigDecimal) {
          return ((BigDecimal) obj).doubleValue();
        }
        break;
      case LONG:
        if (obj instanceof Number) {
          // widening conversion
          return ((Number) obj).longValue();
        } else if (obj instanceof HiveDecimal) {
          return ((HiveDecimal) obj).longValue();
        } else if (obj instanceof String) {
          return Long.valueOf(obj.toString());
        }
        break;
      case STRING:
        return (obj.toString());
      case TIMESTAMP:
        if (obj instanceof Timestamp) {
          return obj;
        } else if (obj instanceof Integer) {
          return new Timestamp(((Number) obj).longValue());
        } else if (obj instanceof Float) {
          return TimestampUtils.doubleToTimestamp(((Float) obj).doubleValue());
        } else if (obj instanceof Double) {
          return TimestampUtils.doubleToTimestamp(((Double) obj).doubleValue());
        } else if (obj instanceof HiveDecimal) {
          return TimestampUtils.decimalToTimestamp((HiveDecimal) obj);
        } else if (obj instanceof HiveDecimalWritable) {
          return TimestampUtils.decimalToTimestamp(((HiveDecimalWritable) obj).getHiveDecimal());
        } else if (obj instanceof Date) {
          return new Timestamp(((Date) obj).getTime());
        }
        // float/double conversion to timestamp is interpreted as seconds whereas integer conversion
        // to timestamp is interpreted as milliseconds by default. The integer to timestamp casting
        // is also config driven. The filter operator changes its promotion based on config:
        // "int.timestamp.conversion.in.seconds". Disable PPD for integer cases.
        break;
      default:
        break;
    }

    throw new SargCastException(String.format(
        "ORC SARGS could not convert from %s to %s", obj.getClass()
            .getSimpleName(), type));
  }

  public static class SargApplier {
    public final static boolean[] READ_ALL_RGS = null;
    public final static boolean[] READ_NO_RGS = new boolean[0];

    private final OrcFile.WriterVersion writerVersion;
    private final SearchArgument sarg;
    private final List<PredicateLeaf> sargLeaves;
    private final int[] filterColumns;
    private final long rowIndexStride;
    // same as the above array, but indices are set to true
    private final boolean[] sargColumns;
    private SchemaEvolution evolution;
    private final long[] exceptionCount;
    private final boolean useUTCTimestamp;
    private final boolean writerUsedProlepticGregorian;
    private final boolean convertToProlepticGregorian;

    public SargApplier(SearchArgument sarg,
                       long rowIndexStride,
                       SchemaEvolution evolution,
                       OrcFile.WriterVersion writerVersion,
                       boolean useUTCTimestamp,
                       boolean writerUsedProlepticGregorian,
                       boolean convertToProlepticGregorian) {
      this.writerVersion = writerVersion;
      this.sarg = sarg;
      sargLeaves = sarg.getLeaves();
      this.writerUsedProlepticGregorian = writerUsedProlepticGregorian;
      this.convertToProlepticGregorian = convertToProlepticGregorian;
      filterColumns = mapSargColumnsToOrcInternalColIdx(sargLeaves,
                                                        evolution);
      this.rowIndexStride = rowIndexStride;
      // included will not be null, row options will fill the array with
      // trues if null
      sargColumns = new boolean[evolution.getFileIncluded().length];
      for (int i : filterColumns) {
        // filter columns may have -1 as index which could be partition
        // column in SARG.
        if (i > 0) {
          sargColumns[i] = true;
        }
      }
      this.evolution = evolution;
      exceptionCount = new long[sargLeaves.size()];
      this.useUTCTimestamp = useUTCTimestamp;
    }

    /**
     * Pick the row groups that we need to load from the current stripe.
     *
     * @return an array with a boolean for each row group or null if all of the
     * row groups must be read.
     * @throws IOException
     */
    public boolean[] pickRowGroups(StripeInformation stripe,
                                   OrcProto.RowIndex[] indexes,
                                   OrcProto.Stream.Kind[] bloomFilterKinds,
                                   List<OrcProto.ColumnEncoding> encodings,
                                   OrcProto.BloomFilterIndex[] bloomFilterIndices,
                                   boolean returnNone) throws IOException {
      long rowsInStripe = stripe.getNumberOfRows();
      int groupsInStripe = (int) ((rowsInStripe + rowIndexStride - 1) / rowIndexStride);
      boolean[] result = new boolean[groupsInStripe]; // TODO: avoid alloc?
      TruthValue[] leafValues = new TruthValue[sargLeaves.size()];
      boolean hasSelected = false;
      boolean hasSkipped = false;
      TruthValue[] exceptionAnswer = new TruthValue[leafValues.length];
      for (int rowGroup = 0; rowGroup < result.length; ++rowGroup) {
        for (int pred = 0; pred < leafValues.length; ++pred) {
          int columnIx = filterColumns[pred];
          if (columnIx == -1) {
            // the column is a virtual column
            leafValues[pred] = TruthValue.YES_NO_NULL;
          } else if (exceptionAnswer[pred] != null) {
            leafValues[pred] = exceptionAnswer[pred];
          } else {
            if (indexes[columnIx] == null) {
              throw new AssertionError("Index is not populated for " + columnIx);
            }
            OrcProto.RowIndexEntry entry = indexes[columnIx].getEntry(rowGroup);
            if (entry == null) {
              throw new AssertionError("RG is not populated for " + columnIx + " rg " + rowGroup);
            }
            OrcProto.ColumnStatistics stats = entry.getStatistics();
            OrcProto.BloomFilter bf = null;
            OrcProto.Stream.Kind bfk = null;
            if (bloomFilterIndices != null && bloomFilterIndices[columnIx] != null) {
              bfk = bloomFilterKinds[columnIx];
              bf = bloomFilterIndices[columnIx].getBloomFilter(rowGroup);
            }
            if (evolution != null && evolution.isPPDSafeConversion(columnIx)) {
              PredicateLeaf predicate = sargLeaves.get(pred);
              try {
                leafValues[pred] = evaluatePredicateProto(stats,
                    predicate, bfk, encodings.get(columnIx), bf,
                    writerVersion, evolution.getFileSchema().
                    findSubtype(columnIx),
                    useUTCTimestamp,
                    writerUsedProlepticGregorian,
                    convertToProlepticGregorian);
              } catch (Exception e) {
                exceptionCount[pred] += 1;
                if (e instanceof SargCastException) {
                  LOG.info("Skipping ORC PPD - " + e.getMessage() + " on "
                      + predicate);
                } else {
                  if (LOG.isWarnEnabled()) {
                    final String reason = e.getClass().getSimpleName() + " when evaluating predicate." +
                        " Skipping ORC PPD." +
                        " Stats: " + stats +
                        " Predicate: " + predicate;
                    LOG.warn(reason, e);
                  }
                }
                boolean hasNoNull = stats.hasHasNull() && !stats.getHasNull();
                if (predicate.getOperator().equals(PredicateLeaf.Operator.NULL_SAFE_EQUALS)
                    || hasNoNull) {
                  exceptionAnswer[pred] = TruthValue.YES_NO;
                } else {
                  exceptionAnswer[pred] = TruthValue.YES_NO_NULL;
                }
                leafValues[pred] = exceptionAnswer[pred];
              }
            } else {
              leafValues[pred] = TruthValue.YES_NO_NULL;
            }
            if (LOG.isTraceEnabled()) {
              LOG.trace("Stats = " + stats);
              LOG.trace("Setting " + sargLeaves.get(pred) + " to " + leafValues[pred]);
            }
          }
        }
        result[rowGroup] = sarg.evaluate(leafValues).isNeeded();
        hasSelected = hasSelected || result[rowGroup];
        hasSkipped = hasSkipped || (!result[rowGroup]);
        if (LOG.isDebugEnabled()) {
          LOG.debug("Row group " + (rowIndexStride * rowGroup) + " to " +
              (rowIndexStride * (rowGroup + 1) - 1) + " is " +
              (result[rowGroup] ? "" : "not ") + "included.");
        }
      }

      return hasSkipped ? ((hasSelected || !returnNone) ? result : READ_NO_RGS) : READ_ALL_RGS;
    }

    /**
     * Get the count of exceptions for testing.
     * @return
     */
    long[] getExceptionCount() {
      return exceptionCount;
    }
  }

  /**
   * Pick the row groups that we need to load from the current stripe.
   *
   * @return an array with a boolean for each row group or null if all of the
   * row groups must be read.
   * @throws IOException
   */
  protected boolean[] pickRowGroups() throws IOException {
    // if we don't have a sarg or indexes, we read everything
    if (sargApp == null) {
      return null;
    }
    readRowIndex(currentStripe, fileIncluded, sargApp.sargColumns);
    return sargApp.pickRowGroups(stripes.get(currentStripe), indexes,
        bloomFilterKind, stripeFooter.getColumnsList(), bloomFilterIndices, false);
  }

  private void clearStreams() {
    // explicit close of all streams to de-ref ByteBuffers
    for (InStream is : streams.values()) {
      is.close();
    }
    if (bufferChunks != null) {
      if (dataReader.isTrackingDiskRanges()) {
        for (DiskRangeList range = bufferChunks; range != null; range = range.next) {
          if (!(range instanceof BufferChunk)) {
            continue;
          }
          dataReader.releaseBuffer(((BufferChunk) range).getChunk());
        }
      }
    }
    bufferChunks = null;
    streams.clear();
  }

  /**
   * Read the current stripe into memory.
   *
   * @throws IOException
   */
  private void readStripe() throws IOException {
    StripeInformation stripe = beginReadStripe();
    includedRowGroups = pickRowGroups();

    // move forward to the first unskipped row
    if (includedRowGroups != null) {
      while (rowInStripe < rowCountInStripe &&
          !includedRowGroups[(int) (rowInStripe / rowIndexStride)]) {
        rowInStripe = Math.min(rowCountInStripe, rowInStripe + rowIndexStride);
      }
    }

    // if we haven't skipped the whole stripe, read the data
    if (rowInStripe < rowCountInStripe) {
      // if we aren't projecting columns or filtering rows, just read it all
      if (isFullRead() && includedRowGroups == null) {
        readAllDataStreams(stripe);
      } else {
        readPartialDataStreams(stripe);
      }
      reader.startStripe(streams, stripeFooter);
      // if we skipped the first row group, move the pointers forward
      if (rowInStripe != 0) {
        seekToRowEntry(reader, (int) (rowInStripe / rowIndexStride));
      }
    }
  }

  private boolean isFullRead() {
    for (boolean isColumnPresent : fileIncluded){
      if (!isColumnPresent){
        return false;
      }
    }
    return true;
  }

  private StripeInformation beginReadStripe() throws IOException {
    StripeInformation stripe = stripes.get(currentStripe);
    stripeFooter = readStripeFooter(stripe);
    clearStreams();
    // setup the position in the stripe
    rowCountInStripe = stripe.getNumberOfRows();
    rowInStripe = 0;
    rowBaseInStripe = 0;
    for (int i = 0; i < currentStripe; ++i) {
      rowBaseInStripe += stripes.get(i).getNumberOfRows();
    }
    // reset all of the indexes
    for (int i = 0; i < indexes.length; ++i) {
      indexes[i] = null;
    }
    return stripe;
  }

  private void readAllDataStreams(StripeInformation stripe) throws IOException {
    long start = stripe.getIndexLength();
    long end = start + stripe.getDataLength();
    // explicitly trigger 1 big read
    DiskRangeList toRead = new DiskRangeList(start, end);
    bufferChunks = dataReader.readFileData(toRead, stripe.getOffset(), false);
    List<OrcProto.Stream> streamDescriptions = stripeFooter.getStreamsList();
    createStreams(streamDescriptions, bufferChunks, null,
        dataReader.getCompressionCodec(), bufferSize, streams);
  }

  /**
   * Plan the ranges of the file that we need to read given the list of
   * columns and row groups.
   *
   * @param streamList        the list of streams available
   * @param indexes           the indexes that have been loaded
   * @param includedColumns   which columns are needed
   * @param includedRowGroups which row groups are needed
   * @param isCompressed      does the file have generic compression
   * @param encodings         the encodings for each column
   * @param types             the types of the columns
   * @param compressionSize   the compression block size
   * @return the list of disk ranges that will be loaded
   */
  static DiskRangeList planReadPartialDataStreams
  (List<OrcProto.Stream> streamList,
      OrcProto.RowIndex[] indexes,
      boolean[] includedColumns,
      boolean[] includedRowGroups,
      boolean isCompressed,
      List<OrcProto.ColumnEncoding> encodings,
      List<OrcProto.Type> types,
      int compressionSize,
      boolean doMergeBuffers) {
    long offset = 0;
    // figure out which columns have a present stream
    boolean[] hasNull = RecordReaderUtils.findPresentStreamsByColumn(streamList, types);
    CreateHelper list = new CreateHelper();
    for (OrcProto.Stream stream : streamList) {
      long length = stream.getLength();
      int column = stream.getColumn();
      OrcProto.Stream.Kind streamKind = stream.getKind();
      // since stream kind is optional, first check if it exists
      if (stream.hasKind() &&
          (StreamName.getArea(streamKind) == StreamName.Area.DATA) &&
          (column < includedColumns.length && includedColumns[column])) {
        // if we aren't filtering or it is a dictionary, load it.
        if (includedRowGroups == null
            || RecordReaderUtils.isDictionary(streamKind, encodings.get(column))) {
          RecordReaderUtils.addEntireStreamToRanges(offset, length, list, doMergeBuffers);
        } else {
          RecordReaderUtils.addRgFilteredStreamToRanges(stream, includedRowGroups,
              isCompressed, indexes[column], encodings.get(column), types.get(column),
              compressionSize, hasNull[column], offset, length, list, doMergeBuffers);
        }
      }
      offset += length;
    }
    return list.extract();
  }

  void createStreams(List<OrcProto.Stream> streamDescriptions,
      DiskRangeList ranges,
      boolean[] includeColumn,
      CompressionCodec codec,
      int bufferSize,
      Map<StreamName, InStream> streams) throws IOException {
    long streamOffset = 0;
    for (OrcProto.Stream streamDesc : streamDescriptions) {
      int column = streamDesc.getColumn();
      if ((includeColumn != null &&
          (column < includeColumn.length && !includeColumn[column])) ||
          streamDesc.hasKind() &&
              (StreamName.getArea(streamDesc.getKind()) != StreamName.Area.DATA)) {
        streamOffset += streamDesc.getLength();
        continue;
      }
      List<DiskRange> buffers = RecordReaderUtils.getStreamBuffers(
          ranges, streamOffset, streamDesc.getLength());
      StreamName name = new StreamName(column, streamDesc.getKind());
      streams.put(name, InStream.create(name.toString(), buffers,
          streamDesc.getLength(), codec, bufferSize));
      streamOffset += streamDesc.getLength();
    }
  }

  private void readPartialDataStreams(StripeInformation stripe) throws IOException {
    List<OrcProto.Stream> streamList = stripeFooter.getStreamsList();
    DiskRangeList toRead = planReadPartialDataStreams(streamList,
        indexes, fileIncluded, includedRowGroups, dataReader.getCompressionCodec() != null,
        stripeFooter.getColumnsList(), types, bufferSize, true);
    if (LOG.isDebugEnabled()) {
      LOG.debug("chunks = " + RecordReaderUtils.stringifyDiskRanges(toRead));
    }
    bufferChunks = dataReader.readFileData(toRead, stripe.getOffset(), false);
    if (LOG.isDebugEnabled()) {
      LOG.debug("merge = " + RecordReaderUtils.stringifyDiskRanges(bufferChunks));
    }

    createStreams(streamList, bufferChunks, fileIncluded,
        dataReader.getCompressionCodec(), bufferSize, streams);
  }

  /**
   * Read the next stripe until we find a row that we don't skip.
   *
   * @throws IOException
   */
  private void advanceStripe() throws IOException {
    rowInStripe = rowCountInStripe;
    while (rowInStripe >= rowCountInStripe &&
        currentStripe < stripes.size() - 1) {
      currentStripe += 1;
      readStripe();
    }
  }

  /**
   * Skip over rows that we aren't selecting, so that the next row is
   * one that we will read.
   *
   * @param nextRow the row we want to go to
   * @throws IOException
   */
  private boolean advanceToNextRow(
      TreeReaderFactory.TreeReader reader, long nextRow, boolean canAdvanceStripe)
      throws IOException {
    long nextRowInStripe = nextRow - rowBaseInStripe;
    // check for row skipping
    if (rowIndexStride != 0 &&
        includedRowGroups != null &&
        nextRowInStripe < rowCountInStripe) {
      int rowGroup = (int) (nextRowInStripe / rowIndexStride);
      if (!includedRowGroups[rowGroup]) {
        while (rowGroup < includedRowGroups.length && !includedRowGroups[rowGroup]) {
          rowGroup += 1;
        }
        if (rowGroup >= includedRowGroups.length) {
          if (canAdvanceStripe) {
            advanceStripe();
          }
          return canAdvanceStripe;
        }
        nextRowInStripe = Math.min(rowCountInStripe, rowGroup * rowIndexStride);
      }
    }
    if (nextRowInStripe >= rowCountInStripe) {
      if (canAdvanceStripe) {
        advanceStripe();
      }
      return canAdvanceStripe;
    }
    if (nextRowInStripe != rowInStripe) {
      if (rowIndexStride != 0) {
        int rowGroup = (int) (nextRowInStripe / rowIndexStride);
        seekToRowEntry(reader, rowGroup);
        reader.skipRows(nextRowInStripe - rowGroup * rowIndexStride);
      } else {
        reader.skipRows(nextRowInStripe - rowInStripe);
      }
      rowInStripe = nextRowInStripe;
    }
    return true;
  }

  @Override
  public boolean nextBatch(VectorizedRowBatch batch) throws IOException {
    try {
      if (rowInStripe >= rowCountInStripe) {
        currentStripe += 1;
        if (currentStripe >= stripes.size()) {
          batch.size = 0;
          return false;
        }
        readStripe();
      }

      int batchSize = computeBatchSize(batch.getMaxSize());

      rowInStripe += batchSize;
      reader.setVectorColumnCount(batch.getDataColumnCount());
      reader.nextBatch(batch, batchSize);
      batch.selectedInUse = false;
      batch.size = batchSize;
      advanceToNextRow(reader, rowInStripe + rowBaseInStripe, true);
      return batch.size  != 0;
    } catch (IOException e) {
      // Rethrow exception with file name in log message
      throw new IOException("Error reading file: " + path, e);
    }
  }

  private int computeBatchSize(long targetBatchSize) {
    final int batchSize;
    // In case of PPD, batch size should be aware of row group boundaries. If only a subset of row
    // groups are selected then marker position is set to the end of range (subset of row groups
    // within strip). Batch size computed out of marker position makes sure that batch size is
    // aware of row group boundary and will not cause overflow when reading rows
    // illustration of this case is here https://issues.apache.org/jira/browse/HIVE-6287
    if (rowIndexStride != 0 && includedRowGroups != null && rowInStripe < rowCountInStripe) {
      int startRowGroup = (int) (rowInStripe / rowIndexStride);
      if (!includedRowGroups[startRowGroup]) {
        while (startRowGroup < includedRowGroups.length && !includedRowGroups[startRowGroup]) {
          startRowGroup += 1;
        }
      }

      int endRowGroup = startRowGroup;
      while (endRowGroup < includedRowGroups.length && includedRowGroups[endRowGroup]) {
        endRowGroup += 1;
      }

      final long markerPosition =
          (endRowGroup * rowIndexStride) < rowCountInStripe ? (endRowGroup * rowIndexStride)
              : rowCountInStripe;
      batchSize = (int) Math.min(targetBatchSize, (markerPosition - rowInStripe));

      if (isLogDebugEnabled && batchSize < targetBatchSize) {
        LOG.debug("markerPosition: " + markerPosition + " batchSize: " + batchSize);
      }
    } else {
      batchSize = (int) Math.min(targetBatchSize, (rowCountInStripe - rowInStripe));
    }
    return batchSize;
  }

  @Override
  public void close() throws IOException {
    clearStreams();
    dataReader.close();
  }

  @Override
  public long getRowNumber() {
    return rowInStripe + rowBaseInStripe + firstRow;
  }

  /**
   * Return the fraction of rows that have been read from the selected.
   * section of the file
   *
   * @return fraction between 0.0 and 1.0 of rows consumed
   */
  @Override
  public float getProgress() {
    return ((float) rowBaseInStripe + rowInStripe) / totalRowCount;
  }

  private int findStripe(long rowNumber) {
    for (int i = 0; i < stripes.size(); i++) {
      StripeInformation stripe = stripes.get(i);
      if (stripe.getNumberOfRows() > rowNumber) {
        return i;
      }
      rowNumber -= stripe.getNumberOfRows();
    }
    throw new IllegalArgumentException("Seek after the end of reader range");
  }

  public OrcIndex readRowIndex(int stripeIndex, boolean[] included,
                               boolean[] sargColumns) throws IOException {
    return readRowIndex(stripeIndex, included, null, null, sargColumns);
  }

  public OrcIndex readRowIndex(int stripeIndex, boolean[] included,
                               OrcProto.RowIndex[] indexes,
                               OrcProto.BloomFilterIndex[] bloomFilterIndex,
                               boolean[] sargColumns) throws IOException {
    StripeInformation stripe = stripes.get(stripeIndex);
    OrcProto.StripeFooter stripeFooter = null;
    // if this is the current stripe, use the cached objects.
    if (stripeIndex == currentStripe) {
      stripeFooter = this.stripeFooter;
      indexes = indexes == null ? this.indexes : indexes;
      bloomFilterIndex = bloomFilterIndex == null ? this.bloomFilterIndices : bloomFilterIndex;
      sargColumns = sargColumns == null ?
          (sargApp == null ? null : sargApp.sargColumns) : sargColumns;
    }
    return dataReader.readRowIndex(stripe, evolution.getFileType(0), stripeFooter,
        ignoreNonUtf8BloomFilter, included, indexes, sargColumns, writerVersion,
        bloomFilterKind, bloomFilterIndex);
  }

  private void seekToRowEntry(TreeReaderFactory.TreeReader reader, int rowEntry)
      throws IOException {
    PositionProvider[] index = new PositionProvider[indexes.length];
    for (int i = 0; i < indexes.length; ++i) {
      if (indexes[i] != null) {
        OrcProto.RowIndexEntry entry = indexes[i].getEntry(rowEntry);
        // This is effectively a test for pre-ORC-569 files.
        if (rowEntry == 0 && entry.getPositionsCount() == 0) {
          index[i] = new ZeroPositionProvider();
        } else {
          index[i] = new PositionProviderImpl(entry);
        }
      }
    }
    reader.seek(index);
  }

  @Override
  public void seekToRow(long rowNumber) throws IOException {
    if (rowNumber < 0) {
      throw new IllegalArgumentException("Seek to a negative row number " +
          rowNumber);
    } else if (rowNumber < firstRow) {
      throw new IllegalArgumentException("Seek before reader range " +
          rowNumber);
    }
    // convert to our internal form (rows from the beginning of slice)
    rowNumber -= firstRow;

    // move to the right stripe
    int rightStripe = findStripe(rowNumber);
    if (rightStripe != currentStripe) {
      currentStripe = rightStripe;
      readStripe();
    }
    readRowIndex(currentStripe, fileIncluded, sargApp == null ? null : sargApp.sargColumns);

    // if we aren't to the right row yet, advance in the stripe.
    advanceToNextRow(reader, rowNumber, true);
  }

  private static final String TRANSLATED_SARG_SEPARATOR = "_";
  public static String encodeTranslatedSargColumn(int rootColumn, Integer indexInSourceTable) {
    return rootColumn + TRANSLATED_SARG_SEPARATOR
        + ((indexInSourceTable == null) ? -1 : indexInSourceTable);
  }

  public static int[] mapTranslatedSargColumns(
      List<OrcProto.Type> types, List<PredicateLeaf> sargLeaves) {
    int[] result = new int[sargLeaves.size()];
    OrcProto.Type lastRoot = null; // Root will be the same for everyone as of now.
    String lastRootStr = null;
    for (int i = 0; i < result.length; ++i) {
      String[] rootAndIndex = sargLeaves.get(i).getColumnName().split(TRANSLATED_SARG_SEPARATOR);
      assert rootAndIndex.length == 2;
      String rootStr = rootAndIndex[0], indexStr = rootAndIndex[1];
      int index = Integer.parseInt(indexStr);
      // First, check if the column even maps to anything.
      if (index == -1) {
        result[i] = -1;
        continue;
      }
      assert index >= 0;
      // Then, find the root type if needed.
      if (!rootStr.equals(lastRootStr)) {
        lastRoot = types.get(Integer.parseInt(rootStr));
        lastRootStr = rootStr;
      }
      // Subtypes of the root types correspond, in order, to the columns in the table schema
      // (disregarding schema evolution that doesn't presently work). Get the index for the
      // corresponding subtype.
      result[i] = lastRoot.getSubtypes(index);
    }
    return result;
  }

  public CompressionCodec getCompressionCodec() {
    return dataReader.getCompressionCodec();
  }

  public int getMaxDiskRangeChunkLimit() {
    return maxDiskRangeChunkLimit;
  }
}
