# 指定cmake的最小版本
cmake_minimum_required(VERSION 3.4.1)

include_directories(src/main/cpp/libyuv/include)

add_subdirectory(src/main/cpp/libyuv)
# 创建一个静态或动态库
# 第一个参数：库的名称
# 第二个参数：库的类别，静态的还是动态的
# 第三个参数：库的源文件的路径
add_library( native-lib
             SHARED
        src/main/cpp/native-lib.cpp)

# 找到一个预编译的库
find_library(log-lib
              log )
# 链接到目标库
target_link_libraries( native-lib
                       ${log-lib}
                       yuv)