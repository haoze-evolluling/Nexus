cmake_minimum_required(VERSION 3.22.1)
project(steamvoice_native C)

set(OPUS_BUILD_PROGRAMS OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_TESTING OFF CACHE BOOL "" FORCE)
set(OPUS_BUILD_SHARED_LIBRARY OFF CACHE BOOL "" FORCE)
set(OPUS_INSTALL_PKG_CONFIG_MODULE OFF CACHE BOOL "" FORCE)
set(OPUS_INSTALL_CMAKE_CONFIG_MODULE OFF CACHE BOOL "" FORCE)
add_subdirectory(third_party/opus opus-build EXCLUDE_FROM_ALL)

add_library(steamvoice_native SHARED opus_jni.c)
target_include_directories(steamvoice_native PRIVATE third_party/opus/include)
target_link_libraries(steamvoice_native opus log)
target_compile_definitions(steamvoice_native PRIVATE OPUS_BUILD)
