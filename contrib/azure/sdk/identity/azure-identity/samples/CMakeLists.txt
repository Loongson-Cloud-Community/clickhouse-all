# Copyright (c) Microsoft Corporation. All rights reserved.
# SPDX-License-Identifier: MIT

cmake_minimum_required (VERSION 3.13)

project (azure-identity-samples LANGUAGES CXX)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED True)

add_executable(client_secret_credential_sample client_secret_credential.cpp)
target_link_libraries(client_secret_credential_sample PRIVATE azure-identity)
target_include_directories(client_secret_credential_sample PRIVATE .)

add_executable(environment_credential_sample environment_credential.cpp)
target_link_libraries(environment_credential_sample PRIVATE azure-identity)
target_include_directories(environment_credential_sample PRIVATE .)

add_executable(managed_identity_credential_sample managed_identity_credential.cpp)
target_link_libraries(managed_identity_credential_sample PRIVATE azure-identity)
target_include_directories(managed_identity_credential_sample PRIVATE .)
