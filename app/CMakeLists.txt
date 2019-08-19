cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/libyuv/include)

add_subdirectory(src/main/cpp/libyuv)

add_library( native-lib
             SHARED
        src/main/cpp/native-lib.cpp)

find_library(log-lib
              log )

target_link_libraries( native-lib
                       ${log-lib}
                       yuv)