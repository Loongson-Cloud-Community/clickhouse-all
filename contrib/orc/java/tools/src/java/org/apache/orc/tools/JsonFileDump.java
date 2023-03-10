/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.orc.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.orc.CompressionKind;
import org.apache.orc.OrcFile;
import org.apache.orc.Reader;
import org.apache.orc.TypeDescription;
import org.apache.orc.impl.AcidStats;
import org.apache.orc.impl.OrcAcidUtils;
import org.apache.orc.impl.ReaderImpl;
import org.apache.orc.impl.RecordReaderImpl;
import org.apache.orc.util.BloomFilter;
import org.codehaus.jettison.json.JSONArray;
import org.apache.orc.util.BloomFilterIO;
import org.apache.orc.BinaryColumnStatistics;
import org.apache.orc.BooleanColumnStatistics;
import org.apache.orc.ColumnStatistics;
import org.apache.orc.impl.ColumnStatisticsImpl;
import org.apache.orc.DateColumnStatistics;
import org.apache.orc.DecimalColumnStatistics;
import org.apache.orc.DoubleColumnStatistics;
import org.apache.orc.IntegerColumnStatistics;
import org.apache.orc.impl.OrcIndex;
import org.apache.orc.OrcProto;
import org.apache.orc.StringColumnStatistics;
import org.apache.orc.StripeInformation;
import org.apache.orc.StripeStatistics;
import org.apache.orc.TimestampColumnStatistics;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONStringer;
import org.codehaus.jettison.json.JSONWriter;

/**
 * File dump tool with json formatted output.
 */
public class JsonFileDump {

  public static void printJsonMetaData(List<String> files,
      Configuration conf,
      List<Integer> rowIndexCols, boolean prettyPrint, boolean printTimeZone)
      throws JSONException, IOException {
    if (files.isEmpty()) {
      return;
    }
    JSONStringer writer = new JSONStringer();
    boolean multiFile = files.size() > 1;
    if (multiFile) {
      writer.array();
    } else {
      writer.object();
    }
    for (String filename : files) {
      try {
        if (multiFile) {
          writer.object();
        }
        writer.key("fileName").value(filename);
        Path path = new Path(filename);
        Reader reader = FileDump.getReader(path, conf, null);
        if (reader == null) {
          writer.key("status").value("FAILED");
          continue;
        }
        writer.key("fileVersion").value(reader.getFileVersion().getName());
        writer.key("writerVersion").value(reader.getWriterVersion());
        RecordReaderImpl rows = (RecordReaderImpl) reader.rows();
        writer.key("numberOfRows").value(reader.getNumberOfRows());
        writer.key("compression").value(reader.getCompressionKind());
        if (reader.getCompressionKind() != CompressionKind.NONE) {
          writer.key("compressionBufferSize").value(reader.getCompressionSize());
        }
        writer.key("schemaString").value(reader.getSchema().toString());
        writer.key("schema").array();
        writeSchema(writer, reader.getTypes());
        writer.endArray();

        writer.key("calendar").value(reader.writerUsedProlepticGregorian()
                                         ? "proleptic Gregorian"
                                         : "Julian/Gregorian");
        writer.key("stripeStatistics").array();
        List<StripeStatistics> stripeStatistics = reader.getStripeStatistics();
        for (int n = 0; n < stripeStatistics.size(); n++) {
          writer.object();
          writer.key("stripeNumber").value(n + 1);
          StripeStatistics ss = stripeStatistics.get(n);
          writer.key("columnStatistics").array();
          for (int i = 0; i < ss.getColumnStatistics().length; i++) {
            writer.object();
            writer.key("columnId").value(i);
            writeColumnStatistics(writer, ss.getColumnStatistics()[i]);
            writer.endObject();
          }
          writer.endArray();
          writer.endObject();
        }
        writer.endArray();

        ColumnStatistics[] stats = reader.getStatistics();
        int colCount = stats.length;
        if (rowIndexCols == null) {
          rowIndexCols = new ArrayList<>(colCount);
          for (int i = 0; i < colCount; ++i) {
            rowIndexCols.add(i);
          }
        }
        writer.key("fileStatistics").array();
        for (int i = 0; i < stats.length; ++i) {
          writer.object();
          writer.key("columnId").value(i);
          writeColumnStatistics(writer, stats[i]);
          writer.endObject();
        }
        writer.endArray();

        writer.key("stripes").array();
        int stripeIx = -1;
        for (StripeInformation stripe : reader.getStripes()) {
          ++stripeIx;
          long stripeStart = stripe.getOffset();
          OrcProto.StripeFooter footer = rows.readStripeFooter(stripe);
          writer.object(); // start of stripe information
          writer.key("stripeNumber").value(stripeIx + 1);
          writer.key("stripeInformation");
          writeStripeInformation(writer, stripe);
          if (printTimeZone) {
            writer.key("writerTimezone").value(
                footer.hasWriterTimezone() ? footer.getWriterTimezone() : FileDump.UNKNOWN);
          }
          long sectionStart = stripeStart;

          writer.key("streams").array();
          for (OrcProto.Stream section : footer.getStreamsList()) {
            writer.object();
            String kind = section.hasKind() ? section.getKind().name() : FileDump.UNKNOWN;
            writer.key("columnId").value(section.getColumn());
            writer.key("section").value(kind);
            writer.key("startOffset").value(sectionStart);
            writer.key("length").value(section.getLength());
            sectionStart += section.getLength();
            writer.endObject();
          }
          writer.endArray();

          writer.key("encodings").array();
          for (int i = 0; i < footer.getColumnsCount(); ++i) {
            writer.object();
            OrcProto.ColumnEncoding encoding = footer.getColumns(i);
            writer.key("columnId").value(i);
            writer.key("kind").value(encoding.getKind());
            if (encoding.getKind() == OrcProto.ColumnEncoding.Kind.DICTIONARY ||
                encoding.getKind() == OrcProto.ColumnEncoding.Kind.DICTIONARY_V2) {
              writer.key("dictionarySize").value(encoding.getDictionarySize());
            }
            writer.endObject();
          }
          writer.endArray();
          if (!rowIndexCols.isEmpty()) {
            // include the columns that are specified, only if the columns are included, bloom filter
            // will be read
            boolean[] sargColumns = new boolean[colCount];
            for (int colIdx : rowIndexCols) {
              sargColumns[colIdx] = true;
            }
            OrcIndex indices = rows.readRowIndex(stripeIx, null, sargColumns);
            writer.key("indexes").array();
            for (int col : rowIndexCols) {
              writer.object();
              writer.key("columnId").value(col);
              writeRowGroupIndexes(writer, col, indices.getRowGroupIndex(),
                  reader.getSchema(), (ReaderImpl) reader);
              writeBloomFilterIndexes(writer, col, indices,
                  reader.getWriterVersion(),
                  reader.getSchema().findSubtype(col).getCategory(),
                  footer.getColumns(col));
              writer.endObject();
            }
            writer.endArray();
          }
          writer.endObject(); // end of stripe information
        }
        writer.endArray();

        FileSystem fs = path.getFileSystem(conf);
        long fileLen = fs.getContentSummary(path).getLength();
        long paddedBytes = FileDump.getTotalPaddingSize(reader);
        // empty ORC file is ~45 bytes. Assumption here is file length always >0
        double percentPadding = ((double) paddedBytes / (double) fileLen) * 100;
        writer.key("fileLength").value(fileLen);
        writer.key("paddingLength").value(paddedBytes);
        writer.key("paddingRatio").value(percentPadding);
        AcidStats acidStats = OrcAcidUtils.parseAcidStats(reader);
        if (acidStats != null) {
          writer.key("numInserts").value(acidStats.inserts);
          writer.key("numDeletes").value(acidStats.deletes);
          writer.key("numUpdates").value(acidStats.updates);
        }
        writer.key("status").value("OK");
        rows.close();

        writer.endObject();
      } catch (Throwable e) {
        writer.key("status").value("FAILED");
        throw e;
      }
    }
    if (multiFile) {
      writer.endArray();
    }

    if (prettyPrint) {
      final String prettyJson;
      if (multiFile) {
        JSONArray jsonArray = new JSONArray(writer.toString());
        prettyJson = jsonArray.toString(2);
      } else {
        JSONObject jsonObject = new JSONObject(writer.toString());
        prettyJson = jsonObject.toString(2);
      }
      System.out.println(prettyJson);
    } else {
      System.out.println(writer.toString());
    }
  }

  private static void writeSchema(JSONStringer writer, List<OrcProto.Type> types)
      throws JSONException {
    int i = 0;
    for(OrcProto.Type type : types) {
      writer.object();
      writer.key("columnId").value(i++);
      writer.key("columnType").value(type.getKind());
      if (type.getFieldNamesCount() > 0) {
        writer.key("childColumnNames").array();
        for (String field : type.getFieldNamesList()) {
          writer.value(field);
        }
        writer.endArray();
        writer.key("childColumnIds").array();
        for (Integer colId : type.getSubtypesList()) {
          writer.value(colId);
        }
        writer.endArray();
      }
      if (type.hasPrecision()) {
        writer.key("precision").value(type.getPrecision());
      }

      if (type.hasScale()) {
        writer.key("scale").value(type.getScale());
      }

      if (type.hasMaximumLength()) {
        writer.key("maxLength").value(type.getMaximumLength());
      }
      writer.endObject();
    }
  }

  private static void writeStripeInformation(JSONWriter writer, StripeInformation stripe)
      throws JSONException {
    writer.object();
    writer.key("offset").value(stripe.getOffset());
    writer.key("indexLength").value(stripe.getIndexLength());
    writer.key("dataLength").value(stripe.getDataLength());
    writer.key("footerLength").value(stripe.getFooterLength());
    writer.key("rowCount").value(stripe.getNumberOfRows());
    writer.endObject();
  }

  private static void writeColumnStatistics(JSONWriter writer, ColumnStatistics cs)
      throws JSONException {
    if (cs != null) {
      writer.key("count").value(cs.getNumberOfValues());
      writer.key("hasNull").value(cs.hasNull());
      if (cs.getBytesOnDisk() != 0) {
        writer.key("bytesOnDisk").value(cs.getBytesOnDisk());
      }
      if (cs instanceof BinaryColumnStatistics) {
        writer.key("totalLength").value(((BinaryColumnStatistics) cs).getSum());
        writer.key("type").value(OrcProto.Type.Kind.BINARY);
      } else if (cs instanceof BooleanColumnStatistics) {
        writer.key("trueCount").value(((BooleanColumnStatistics) cs).getTrueCount());
        writer.key("falseCount").value(((BooleanColumnStatistics) cs).getFalseCount());
        writer.key("type").value(OrcProto.Type.Kind.BOOLEAN);
      } else if (cs instanceof IntegerColumnStatistics) {
        writer.key("min").value(((IntegerColumnStatistics) cs).getMinimum());
        writer.key("max").value(((IntegerColumnStatistics) cs).getMaximum());
        if (((IntegerColumnStatistics) cs).isSumDefined()) {
          writer.key("sum").value(((IntegerColumnStatistics) cs).getSum());
        }
        writer.key("type").value(OrcProto.Type.Kind.LONG);
      } else if (cs instanceof DoubleColumnStatistics) {
        writer.key("min").value(((DoubleColumnStatistics) cs).getMinimum());
        writer.key("max").value(((DoubleColumnStatistics) cs).getMaximum());
        writer.key("sum").value(((DoubleColumnStatistics) cs).getSum());
        writer.key("type").value(OrcProto.Type.Kind.DOUBLE);
      } else if (cs instanceof StringColumnStatistics) {
        writer.key("min").value(((StringColumnStatistics) cs).getMinimum());
        writer.key("max").value(((StringColumnStatistics) cs).getMaximum());
        writer.key("totalLength").value(((StringColumnStatistics) cs).getSum());
        writer.key("type").value(OrcProto.Type.Kind.STRING);
      } else if (cs instanceof DateColumnStatistics) {
        if (((DateColumnStatistics) cs).getMaximum() != null) {
          writer.key("min").value(((DateColumnStatistics) cs).getMinimum());
          writer.key("max").value(((DateColumnStatistics) cs).getMaximum());
        }
        writer.key("type").value(OrcProto.Type.Kind.DATE);
      } else if (cs instanceof TimestampColumnStatistics) {
        if (((TimestampColumnStatistics) cs).getMaximum() != null) {
          writer.key("min").value(((TimestampColumnStatistics) cs).getMinimum());
          writer.key("max").value(((TimestampColumnStatistics) cs).getMaximum());
        }
        writer.key("type").value(OrcProto.Type.Kind.TIMESTAMP);
      } else if (cs instanceof DecimalColumnStatistics) {
        if (((DecimalColumnStatistics) cs).getMaximum() != null) {
          writer.key("min").value(((DecimalColumnStatistics) cs).getMinimum());
          writer.key("max").value(((DecimalColumnStatistics) cs).getMaximum());
          writer.key("sum").value(((DecimalColumnStatistics) cs).getSum());
        }
        writer.key("type").value(OrcProto.Type.Kind.DECIMAL);
      }
    }
  }

  private static void writeBloomFilterIndexes(JSONWriter writer, int col,
                                              OrcIndex index,
                                              OrcFile.WriterVersion version,
                                              TypeDescription.Category type,
                                              OrcProto.ColumnEncoding encoding
                                              ) throws JSONException {

    BloomFilter stripeLevelBF = null;
    OrcProto.BloomFilterIndex[] bloomFilterIndex = index.getBloomFilterIndex();
    if (bloomFilterIndex != null && bloomFilterIndex[col] != null) {
      int entryIx = 0;
      writer.key("bloomFilterIndexes").array();
      for (OrcProto.BloomFilter bf : bloomFilterIndex[col].getBloomFilterList()) {
        writer.object();
        writer.key("entryId").value(entryIx++);
        BloomFilter toMerge = BloomFilterIO.deserialize(
            index.getBloomFilterKinds()[col], encoding, version, type, bf);
        writeBloomFilterStats(writer, toMerge);
        if (stripeLevelBF == null) {
          stripeLevelBF = toMerge;
        } else {
          stripeLevelBF.merge(toMerge);
        }
        writer.endObject();
      }
      writer.endArray();
    }
    if (stripeLevelBF != null) {
      writer.key("stripeLevelBloomFilter");
      writer.object();
      writeBloomFilterStats(writer, stripeLevelBF);
      writer.endObject();
    }
  }

  private static void writeBloomFilterStats(JSONWriter writer, BloomFilter bf)
      throws JSONException {
    int bitCount = bf.getBitSize();
    int popCount = 0;
    for (long l : bf.getBitSet()) {
      popCount += Long.bitCount(l);
    }
    int k = bf.getNumHashFunctions();
    float loadFactor = (float) popCount / (float) bitCount;
    float expectedFpp = (float) Math.pow(loadFactor, k);
    writer.key("numHashFunctions").value(k);
    writer.key("bitCount").value(bitCount);
    writer.key("popCount").value(popCount);
    writer.key("loadFactor").value(loadFactor);
    writer.key("expectedFpp").value(expectedFpp);
  }

  private static void writeRowGroupIndexes(JSONWriter writer, int col,
                                           OrcProto.RowIndex[] rowGroupIndex,
                                           TypeDescription schema,
                                           ReaderImpl reader) throws JSONException {
    OrcProto.RowIndex index;
    if (rowGroupIndex == null || (col >= rowGroupIndex.length) ||
        ((index = rowGroupIndex[col]) == null)) {
      return;
    }

    writer.key("rowGroupIndexes").array();
    for (int entryIx = 0; entryIx < index.getEntryCount(); ++entryIx) {
      writer.object();
      writer.key("entryId").value(entryIx);
      OrcProto.RowIndexEntry entry = index.getEntry(entryIx);
      if (entry == null) {
        continue;
      }
      OrcProto.ColumnStatistics colStats = entry.getStatistics();
      writeColumnStatistics(writer, ColumnStatisticsImpl.deserialize(
          schema.findSubtype(col), colStats, reader));
      writer.key("positions").array();
      for (int posIx = 0; posIx < entry.getPositionsCount(); ++posIx) {
        writer.value(entry.getPositions(posIx));
      }
      writer.endArray();
      writer.endObject();
    }
    writer.endArray();
  }

}
