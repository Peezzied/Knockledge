cmake_minimum_required(VERSION 3.22.1)
project(knockledge_whisper CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

FetchContent_Declare(
    whisper
    URL https://github.com/ggml-org/whisper.cpp/archive/refs/tags/v1.9.5.tar.gz
    URL_HASH SHA256=FF1A9053FEB509FF9D7729703355541AE9690073A6B1C40EB692C962E0DC1720
)

set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(whisper)

set(WHISPER_JNI_LIB knockledge_whisper)

add_library(${WHISPER_JNI_LIB} SHARED whisper_jni.cpp)
target_link_libraries(${WHISPER_JNI_LIB} PRIVATE whisper android log)
target_link_options(${WHISPER_JNI_LIB} PRIVATE -Wl,-z,max-page-size=16384)
