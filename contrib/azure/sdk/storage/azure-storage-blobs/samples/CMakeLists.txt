# Copyright (c) Microsoft Corporation. All rights reserved.
# SPDX-License-Identifier: MIT

cmake_minimum_required (VERSION 3.13)

add_executable(blob-getting-started blob_getting_started.cpp)
target_link_libraries(blob-getting-started PRIVATE azure-storage-blobs)

add_executable(blob-list-operation blob_list_operation.cpp)
target_link_libraries(blob-list-operation  PRIVATE azure-storage-blobs)

add_executable(blob-sas blob_sas.cpp)
target_link_libraries(blob-sas PRIVATE azure-storage-blobs)

add_executable(transactional-checksum transactional_checksum.cpp)
target_link_libraries(transactional-checksum PRIVATE azure-storage-blobs)
