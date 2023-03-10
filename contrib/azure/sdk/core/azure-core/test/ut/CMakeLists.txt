# Copyright (c) Microsoft Corporation. All rights reserved.
# SPDX-License-Identifier: MIT

cmake_minimum_required (VERSION 3.13)

set(azure-core-test)

# Create test data for FileUpload test (100K) by writing 1K * 100 times
set(RANGE 0)
set(1K "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX")
file(WRITE ${CMAKE_BINARY_DIR}/fileData "")
while(RANGE LESS 100)
     file(APPEND ${CMAKE_BINARY_DIR}/fileData "${1K}")
     MATH(EXPR RANGE "${RANGE}+1")
endwhile()
add_compile_definitions(AZURE_TEST_DATA_PATH="${CMAKE_BINARY_DIR}")

add_compile_definitions(AZURE_TEST_RECORDING_DIR="${CMAKE_CURRENT_LIST_DIR}")

project (azure-core-test LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED True)

if(BUILD_TRANSPORT_CURL)
  SET(CURL_OPTIONS_TESTS curl_options_test.cpp)
  SET(CURL_SESSION_TESTS curl_session_test_test.cpp curl_session_test.hpp)
  SET(CURL_CONNECTION_POOL_TESTS curl_connection_pool_test.cpp)
endif()

if(RUN_LONG_UNIT_TESTS)
  add_compile_definitions(RUN_LONG_UNIT_TESTS)
endif()

include(GoogleTest)

add_executable (
  azure-core-test
    azure_core_test.cpp
    base64_test.cpp
    bearer_token_authentication_policy_test.cpp
    bodystream_test.cpp
    case_insensitive_containers_test.cpp
    client_options_test.cpp
    context_test.cpp
    ${CURL_CONNECTION_POOL_TESTS}
    ${CURL_OPTIONS_TESTS}
    ${CURL_SESSION_TESTS}
    datetime_test.cpp
    environmentLogLevelListener_test.cpp
    etag_test.cpp
    http_test.cpp
    http_test.hpp
    http_method_test.cpp
    json_test.cpp
    log_policy_test.cpp
    logging_test.cpp
    macro_guard_test.cpp
    match_conditions_test.cpp
    md5_test.cpp
    modified_conditions_test.cpp
    nullable_test.cpp
    operation_test.cpp
    operation_test.hpp
    operation_status_test.cpp
    pipeline_test.cpp
    policy_test.cpp
    request_id_policy_test.cpp
    response_t_test.cpp
    retry_policy_test.cpp
    sha_test.cpp
    simplified_header_test.cpp
    string_test.cpp
    telemetry_policy_test.cpp
    transport_adapter_base_test.cpp
    transport_adapter_base_test.hpp
    transport_adapter_implementation_test.cpp
    url_test.cpp
    uuid_test.cpp
    exception_test.cpp
)

if (MSVC)
  # Disable warnings:
  # - C26495: Variable
  #             - 'testing::internal::Mutex::critical_section_'
  #             - 'testing::internal::Mutex::critical_section_init_phase_'
  #             - 'testing::internal::Mutex::owner_thread_id_'
  #             - 'testing::internal::Mutex::type_'
  #           is uninitialized. Always initialize member variables (type.6).
  # - C26812: The enum type
  #             - 'testing::internal::Mutex::StaticConstructorSelector'
  #             - 'testing::TestPartResult::Type'
  #           is unscoped. Prefer 'enum class' over 'enum' (Enum.3)
  # - C6326: Google comparisons 
  target_compile_options(azure-core-test PUBLIC /wd26495 /wd26812 /wd6326 /wd28204 /wd28020 /wd6330 /wd4389)
endif()

# Adding private headers from CORE to the tests so we can test the private APIs with no relative paths include.
target_include_directories (azure-core-test PRIVATE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../../src>)

target_link_libraries(azure-core-test PRIVATE azure-core gtest gmock)

## Global context test
add_executable (
  azure-core-global-context-test
  global_context_test.cpp
)
if (MSVC)
  # Disable gtest warnings for MSVC
  target_compile_options(azure-core-global-context-test PUBLIC /wd26495 /wd26812 /wd6326 /wd28204 /wd28020 /wd6330 /wd4389)
endif()
target_link_libraries(azure-core-global-context-test PRIVATE azure-core gtest_main)

# gtest_discover_tests will scan the test from azure-core-test and call add_test
# for each test to ctest. This enables `ctest -r` to run specific tests directly.
gtest_discover_tests(azure-core-test
     TEST_PREFIX azure-core.
     NO_PRETTY_TYPES
     NO_PRETTY_VALUES)

if(BUILD_TRANSPORT_CURL)
  ################## Azure Libcurl Core Test #################################
  # Creating one exe file alone for this test since it requires the full control over the connection pool.
  # This test will check that end-user can call `curl_global_cleanup()` with active handlers in the connection pool
  # without getting errors.
  add_executable (
    azure-core-libcurl-test
    azure_libcurl_core_main_test.cpp
  )

  if (MSVC)
    # warning C4389: '==': signed/unsigned mismatch
    # warning C6326: Google comparisons 
    target_compile_options(azure-core-libcurl-test PUBLIC /wd4389 /wd6326 )
  endif()
  
  # Adding private headers from CORE to the tests so we can test the private APIs with no relative paths include.
  target_include_directories (azure-core-libcurl-test PRIVATE $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/../../src>)

  target_link_libraries(azure-core-libcurl-test PRIVATE azure-core gtest gmock)

  # Use the same prefix to run this test
  gtest_discover_tests(azure-core-libcurl-test
        TEST_PREFIX azure-core.)

endif()
gtest_discover_tests(azure-core-global-context-test
     TEST_PREFIX azure-core.
     NO_PRETTY_TYPES
     NO_PRETTY_VALUES)
