#
# Copyright (c) 2023-2026 Software Architecture Group, Hasso Plattner Institute
# Copyright (c) 2023-2026 Oracle and/or its affiliates
#
# Licensed under the MIT License.
#

cmake_minimum_required(VERSION 3.22)
project(de.hpi.swa.trufflesqueak.ffi.native)

if(NOT DEFINED SRC_DIR)
    set(SRC_DIR "${CMAKE_SOURCE_DIR}")
endif()

set(CMAKE_C_STANDARD 11)

# don't install into the system but into the MX project's output dir
set(CMAKE_INSTALL_PREFIX ${CMAKE_BINARY_DIR})

set(CAPI_SRC "${SRC_DIR}/src")

function(build_library LIB_NAME SOURCE_FILE)
    add_library(${LIB_NAME} SHARED ${SOURCE_FILE})
    target_include_directories(${LIB_NAME} PUBLIC include)
endfunction()


build_library(InterpreterProxy "${CAPI_SRC}/InterpreterProxy.c")
build_library(SqueakFFIPrims "${CAPI_SRC}/SqueakFFIPrims.c")