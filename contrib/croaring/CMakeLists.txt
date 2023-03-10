cmake_minimum_required(VERSION 2.8)
include(GNUInstallDirs)

set(CMAKE_MACOSX_RPATH OFF)
if (NOT CMAKE_BUILD_TYPE)
                message(STATUS "No build type selected, default to Release")
                set(CMAKE_BUILD_TYPE Release CACHE STRING "Choose the type of build." FORCE)
endif()

project(RoaringBitmap)
set(ROARING_LIB_NAME roaring)
set(PROJECT_VERSION_MAJOR 0)
set(PROJECT_VERSION_MINOR 2)
set(PROJECT_VERSION_PATCH 66)
set(ROARING_LIB_VERSION "0.2.66" CACHE STRING "Roaring library version")
set(ROARING_LIB_SOVERSION "0" CACHE STRING "Roaring library soversion")

option(ROARING_DISABLE_X64 "Forcefully disable x64 optimizations even if hardware supports it (this disables AVX) " OFF)
option(ROARING_DISABLE_AVX "Forcefully disable AVX even if hardware supports it" OFF)
option(ROARING_DISABLE_NEON "Forcefully disable NEON even if hardware supports it" OFF)
option(ROARING_DISABLE_NATIVE "Forcefully disable -march optimizations" OFF)
set(ROARING_ARCH "native" CACHE STRING "If ROARING_DISABLE_NATIVE is OFF, the architecture to optimize for (-march)")

IF(${CMAKE_SYSTEM_PROCESSOR} MATCHES "arm")
SET(ROARING_DISABLE_AVX ON) # for ARM processors, there is no hope of having AVX support
SET(ROARING_DISABLE_NATIVE ON) # ARM platforms may not like -march=native
ENDIF(${CMAKE_SYSTEM_PROCESSOR} MATCHES "arm")

if(${CMAKE_SYSTEM_PROCESSOR} MATCHES "aarch64")
SET(ROARING_DISABLE_AVX ON) # for ARM processors, there is no hope of having AVX support
SET(ROARING_DISABLE_NATIVE ON) # ARM platforms may not like -march=native
endif()

option(ROARING_BUILD_STATIC "Build a static library" OFF) # turning it on disables the production of a dynamic library
option(ROARING_LINK_STATIC "Link executables (tests, benchmarks) statically" OFF)
option(ROARING_BUILD_LTO "Build library with Link Time Optimization" OFF)
option(ROARING_SANITIZE "Sanitize addresses" OFF)
option(ENABLE_ROARING_TESTS "If OFF, disable unit tests altogether" ON)

set(CMAKE_MODULE_PATH "${CMAKE_CURRENT_SOURCE_DIR}/tools/cmake")

find_package(CTargets)
find_package(Options)
find_package(LTO)

## C header files get installed to /usr/local/include/roaring typically
install(DIRECTORY include/${ROARING_LIB_NAME} DESTINATION include)

####
### Some users want the C++ header files to be installed as well.
### C++ header files get installed to /usr/local/include/roaring typically
SET(CPP_ROARING_HEADERS cpp/roaring64map.hh  cpp/roaring.hh) # needs to be updated if we add more files
install(FILES ${CPP_ROARING_HEADERS} DESTINATION include/${ROARING_LIB_NAME})


enable_testing()
include(CTest)

##################################
#### My understanding is that variable get passed down to subdirectories, so
#### this needs to be done here.
set (TEST_DATA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/tests/testdata/")
set (BENCHMARK_DATA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/benchmarks/realdata/")
MESSAGE( STATUS "BENCHMARK_DATA_DIR: " ${BENCHMARK_DATA_DIR} )
MESSAGE( STATUS "TEST_DATA_DIR: " ${TEST_DATA_DIR} )
configure_file ("${CMAKE_CURRENT_SOURCE_DIR}/tests/config.h.in"
                "${CMAKE_CURRENT_SOURCE_DIR}/tests/config.h")
#################################

add_subdirectory(src)
if(ENABLE_ROARING_TESTS)
  add_subdirectory(benchmarks)
  add_subdirectory(tests)
endif()
# Being terse is good, but knowing how the build is configured is important
# and should not be hard to figure out.
MESSAGE( STATUS "CMAKE_SYSTEM_PROCESSOR: " ${CMAKE_SYSTEM_PROCESSOR})
MESSAGE( STATUS "CMAKE_BUILD_TYPE: " ${CMAKE_BUILD_TYPE} ) # this tends to be "sticky" so you can remain unknowingly in debug mode
MESSAGE( STATUS "ROARING_DISABLE_X64: " ${ROARING_DISABLE_X64} ) # options in cmake are "sticky" so old options can remain even if that is counterintuitive
MESSAGE( STATUS "ROARING_DISABLE_AVX: " ${ROARING_DISABLE_AVX} ) # options in cmake are "sticky" so old options can remain even if that is counterintuitive
MESSAGE( STATUS "ROARING_DISABLE_NEON: " ${ROARING_DISABLE_NEON} )
MESSAGE( STATUS "ROARING_DISABLE_NATIVE: " ${ROARING_DISABLE_NATIVE} )
MESSAGE( STATUS "ROARING_ARCH: " ${ROARING_ARCH} )
MESSAGE( STATUS "ROARING_BUILD_STATIC: " ${ROARING_BUILD_STATIC} )
MESSAGE( STATUS "ROARING_LINK_STATIC: " ${ROARING_LINK_STATIC} )
MESSAGE( STATUS "ROARING_BUILD_LTO: " ${ROARING_BUILD_LTO} )
MESSAGE( STATUS "ROARING_SANITIZE: " ${ROARING_SANITIZE} )
MESSAGE( STATUS "CMAKE_C_COMPILER: " ${CMAKE_C_COMPILER} ) # important to know which compiler is used
MESSAGE( STATUS "CMAKE_C_FLAGS: " ${CMAKE_C_FLAGS} ) # important to know the flags
MESSAGE( STATUS "CMAKE_C_FLAGS_DEBUG: " ${CMAKE_C_FLAGS_DEBUG} )
MESSAGE( STATUS "CMAKE_C_FLAGS_RELEASE: " ${CMAKE_C_FLAGS_RELEASE} )


set(CPACK_PACKAGE_VENDOR "Daniel Lemire")
set(CPACK_PACKAGE_CONTACT "lemire@gmail.com")
set(CPACK_PACKAGE_DESCRIPTION_SUMMARY "Roaring bitmaps in C")
set(CPACK_PACKAGE_VERSION_MAJOR ${PROJECT_VERSION_MAJOR})
set(CPACK_PACKAGE_VERSION_MINOR ${PROJECT_VERSION_MINOR})
set(CPACK_PACKAGE_VERSION_PATCH ${PROJECT_VERSION_PATCH})
set(CPACK_RESOURCE_FILE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE")
set(CPACK_RESOURCE_FILE_README "${CMAKE_CURRENT_SOURCE_DIR}/README.md")

set(CPACK_RPM_PACKAGE_LICENSE "${CMAKE_CURRENT_SOURCE_DIR}/LICENSE")
set(CPACK_SOURCE_GENERATOR "TGZ;ZIP")
include(CPack)
