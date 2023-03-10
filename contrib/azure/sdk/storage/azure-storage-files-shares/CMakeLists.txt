# Copyright (c) Microsoft Corporation. All rights reserved.
# SPDX-License-Identifier: MIT

cmake_minimum_required (VERSION 3.13)
project(azure-storage-files-shares LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED True)
set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)

list(APPEND CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/../../../cmake-modules")

include(AzureVcpkg)
include(AzureVersion)
include(AzureCodeCoverage)
include(AzureTransportAdapters)
include(AzureDoxygen)
include(AzureGlobalCompileOptions)
include(AzureConfigRTTI)

az_vcpkg_integrate()

if(NOT AZ_ALL_LIBRARIES)
  find_package(azure-storage-common-cpp "12.2.0" CONFIG QUIET)
  if(NOT azure-storage-common-cpp_FOUND)
    find_package(azure-storage-common-cpp "12.2.0" REQUIRED)
  endif()
endif()

set(
  AZURE_STORAGE_FILES_SHARES_HEADER
    inc/azure/storage/files/shares/protocol/share_rest_client.hpp
    inc/azure/storage/files/shares/dll_import_export.hpp
    inc/azure/storage/files/shares/share_client.hpp
    inc/azure/storage/files/shares/share_constants.hpp
    inc/azure/storage/files/shares/share_directory_client.hpp
    inc/azure/storage/files/shares/share_file_attributes.hpp
    inc/azure/storage/files/shares/share_file_client.hpp
    inc/azure/storage/files/shares/share_lease_client.hpp
    inc/azure/storage/files/shares/share_options.hpp
    inc/azure/storage/files/shares/share_responses.hpp
    inc/azure/storage/files/shares/share_sas_builder.hpp
    inc/azure/storage/files/shares/share_service_client.hpp
    inc/azure/storage/files/shares.hpp
)

set(
  AZURE_STORAGE_FILES_SHARES_SOURCE
    src/private/package_version.hpp
    src/share_client.cpp
    src/share_directory_client.cpp
    src/share_file_attributes.cpp
    src/share_file_client.cpp
    src/share_lease_client.cpp
    src/share_responses.cpp
    src/share_rest_client.cpp
    src/share_sas_builder.cpp
    src/share_service_client.cpp
)

add_library(azure-storage-files-shares ${AZURE_STORAGE_FILES_SHARES_HEADER} ${AZURE_STORAGE_FILES_SHARES_SOURCE})

# make sure that users can consume the project as a library.
add_library(Azure::azure-storage-files-shares ALIAS azure-storage-files-shares)

target_include_directories(
  azure-storage-files-shares
    PUBLIC
      $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/inc>
      $<INSTALL_INTERFACE:include>
)

target_link_libraries(azure-storage-files-shares PUBLIC Azure::azure-storage-common)

get_az_version("${CMAKE_CURRENT_SOURCE_DIR}/src/private/package_version.hpp")
generate_documentation(azure-storage-files-shares ${AZ_LIBRARY_VERSION})

az_vcpkg_export(
    azure-storage-files-shares
    STORAGE_FILES_SHARES
    "azure/storage/files/shares/dll_import_export.hpp"
  )

az_rtti_setup(
  azure-storage-files-shares
  STORAGE_FILES_SHARES
  "azure/storage/files/shares/rtti.hpp"
)

# coverage. Has no effect if BUILD_CODE_COVERAGE is OFF
create_code_coverage(storage azure-storage-files-shares azure-storage-test "tests?/*;samples?/*")

if(BUILD_TESTING)
  target_sources(
    azure-storage-test
      PRIVATE
        test/macro_guard.cpp
        test/share_client_test.cpp
        test/share_client_test.hpp
        test/share_directory_client_test.cpp
        test/share_directory_client_test.hpp
        test/share_file_attributes_test.cpp
        test/share_file_client_test.cpp
        test/share_file_client_test.hpp
        test/share_sas_test.cpp
        test/share_service_client_test.cpp
        test/share_service_client_test.hpp
  )

  target_link_libraries(azure-storage-test PRIVATE azure-storage-files-shares)
endif()

if(BUILD_STORAGE_SAMPLES)
  add_subdirectory(samples)
endif()
