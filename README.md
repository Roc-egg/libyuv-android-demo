---
theme: channing-cyan
highlight: arta
---

# 前言

本文主要讲述两点：

1. libyuv编译成Android so库的方法
2. 使用Camera2采集摄像头数据使用libyuv转换格式以及旋转图像

## 开始编译libyuv

编译libyuv没有什么太多难度，不废话直接上步骤

### 编译libyuv系统环境

本人使用的编译环境如下，其他环境请自行测试

```test
Ubuntu20.04.3 + android-ndk-r22b-linux-x86_64
```

### 准备编译脚本开始编译libyuv动态库

1. 创建`yuvInit.sh`脚本

```sh
#!/bin/bash -e

#下载并且解压NDK
NDK_VERSION=android-ndk-r22b
if [ ! -e ${NDK_VERSION}-linux-x86_64.zip ]
 then
    echo "ndk 压缩包不存在"
    wget https://dl.google.com/android/repository/${NDK_VERSION}-linux-x86_64.zip
    #ndk23+后缀不区分x86_64
    #wget https://dl.google.com/android/repository/${NDK_VERSION}-linux.zip
fi

if [ ! -d ${NDK_VERSION} ]
 then
    echo "ndk 目录不存在"
    unzip ${NDK_VERSION}-linux-x86_64.zip
    #ndk23+后缀不区分x86_64
    #unzip ${NDK_VERSION}-linux.zip
fi

#git克隆libyuv库
if [ ! -d libyuv ]
 then
    echo "libyuv 目录不存在"
    git clone https://chromium.googlesource.com/libyuv/libyuv
fi
```

2. 打开终端，在终端输入命令

```
./yuvInit.sh
```

* 会自动下载然后解压指定的`NDK_VERSION`版本
* 然后自动下载libyuv源码文件

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/993ef1b38b52445dab87f05cdddce00e~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=357&h=553&s=178452&e=png&b=390526)

3. 在libyuv文件夹里面创建`Application.mk`文件

```mk
APP_ABI := armeabi-v7a arm64-v8a x86_64
APP_PLATFORM := android-21
APP_CFLAGS += -fexceptions  
APP_STL := c++_static
```

![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ced42393a2314474ad5ab6754052ad3a~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=1158&h=627&s=116426&e=png&b=fcfcfc)

4. 修改libyuv文件夹里面的`Android.mk`文件

    - 因为libyuv直接编译会提示需要依赖libjpeg库
     ![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/1fb9c9b8eefd4e1cbb61a8a65d4d78d4~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=744&h=678&s=86580&e=png&b=300a25)
   - 我们不需要使用直接修改`Android.mk`文件将jpeg相关注释或删
     ![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b9ddbaca86ef4a0c82339ef7a1e4625e~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=974&h=995&s=227126&e=png&b=faf9f9)
   - 顺便把不需要的测试相关编译配置注释或删除掉
     ![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b21572dde6ed4f35a21856745c65e25e~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=980&h=983&s=223402&e=png&b=fbfafa)

5. 配置Android NDK的环境变量

   - 执行命令: `sudo vim /etc/profiles`
   - 输入i进入编辑模式，在最下面添加NDK环境变量
   ```sh
   export NDK_HOME=/home/roc/Desktop/android-ndk-r22b
   export PATH=$NDK_HOME:$PATH
   ```  
   - 然后按Esc键退出编辑，再输入 :wq  执行写入并退出
   - 最后执行命令: source /etc/profile  刷新环境变量配置
   - 验证一下NDK环境变量是否配置正确，输入命令: ndk-build -v
   - 正常可以看到版本号，否则可能得重启一下才能刷新配置

6. 创建编译libyuv脚本文件`yuvBuild.sh`
```sh
#!/bin/bash -e

##最新的NDK使用命令行编译需要制定项目路径，Android.mk，Application.mk文件路径
#NDK_PROJECT_PATH=. 后面是一个英文句号“.”，表示当前路径
NDK_ATTACHED="NDK_PROJECT_PATH=. NDK_APPLICATION_MK=Application.mk APP_BUILD_SCRIPT=Android.mk"

#进入libyuv文件夹
cd libyuv

#先清除之前的编译
ndk-build clean ${NDK_ATTACHED}

#再执行ndk编译
ndk-build ${NDK_ATTACHED}
```
7. 打开终端，在终端输入命令

```
./yuvBuild.sh
```
没有错误显示如下

![image.png](https://p9-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/36d30b141a6340658282f948f97dca91~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=743&h=685&s=145133&e=png&b=310a25)
然后在libyuv文件夹下会生成这两个目录

![image.png](https://p6-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/a628081bf13a47159eff7a8c240931f0~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=1161&h=629&s=119215&e=png&b=fcfcfc)
![image.png](https://p1-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/ecc3d68fa72b4f10974df2e0771df03a~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=1164&h=200&s=44228&e=png&b=fbfbfb)
![image.png](https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/b91dea01fc3645cdb9a20f2cd8fd8e96~tplv-k3u1fbpfcp-jj-mark:0:0:0:0:q75.image#?w=1163&h=241&s=43927&e=png&b=fcfcfc)

### 注意使用Ubuntu编译最新版本libyuv需要注意
上面方法在编译libyuv 1888版本时候正常编译出so库，但是最新版本（1892）编译会显示如下：

![image](https://github.com/user-attachments/assets/c0b7c34b-8f7e-4669-aa06-9f7cc7b864b5)


提示`error: instruction requires: dotprod`缺少点集支持，目前还没找到什么方法，我改用Android Studio集成编译可以正常编译成功，详细见demo

![image](https://github.com/user-attachments/assets/1dbd1eba-83aa-413f-b234-7b528d64c28a)

![image](https://github.com/user-attachments/assets/323ee9b6-ae8b-449d-8885-b78bb56759cc)

![image](https://github.com/user-attachments/assets/c760255a-f699-4b03-9411-501031333051)


添加`libyuvBuild`模块依赖，然后`make`或者`gradle build`出aar包，从aar中提取编译好的`libyuv.so`库
当然你也可以直接在`libyuvBuild`模块里面添加libyuv转换图像相关native方法去使用。

## 使用编译好的libyuv
主要演示从摄像头获取的数据使用我们编译好的libyuv库进行格式转换和旋转翻转处理
### libyuv导入到项目中
![image.png](https://p0-xtjj-private.juejin.cn/tos-cn-i-73owjymdk6/87f37c179ca5497db7d3976da95dd153~tplv-73owjymdk6-jj-mark:0:0:0:0:q75.awebp?policy=eyJ2bSI6MywidWlkIjoiNDA5MDYzNzU5MDk5OTI0NSJ9&rk3s=f64ab15b&x-orig-authkey=f32326d3454f2ac7e96d3d06cdbb035152127018&x-orig-expires=1723520853&x-orig-sign=jRo9ZGMTYTnjR4qmdxO396Apv%2F8%3D)
1. 修改`CMakeLists.txt文件`
```c++
# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
set(libyuvDir ${CMAKE_CURRENT_SOURCE_DIR}/../../../libs/${ANDROID_ABI})

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("androidYuv")

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#
# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(androidYuv SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        libyuv.cpp
)

add_library(yuv SHARED IMPORTED)
set_target_properties(yuv
        PROPERTIES IMPORTED_LOCATION
        ${libyuvDir}/libyuv.so)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(androidYuv
        # List libraries link to the target library
        yuv
        android
        log)
```
2. 创建`YuvUtils.kt`native文件
```kotlin
package com.roc.libyuv

import java.nio.ByteBuffer

/**
 * libYuv native工具类
 */
object YuvUtils {

    init {
        //加载libyuv库
        System.loadLibrary("androidYuv")
    }

    /**
     * 获取版本号
     * @return Int
     */
    external fun libYuvVersion(): Int


    /**
     * 直接将android YUV_420_888转换成ABGR(对应android的ARGB)
     * @param width Int 宽
     * @param height Int 高
     * @param yPlane ByteBuffer image对象Y平面的缓冲
     * @param yRowStride Int Y平面行步进
     * @param uPlane ByteBuffer image对象U平面的缓冲
     * @param uRowStride Int U平面行步进
     * @param vPlane ByteBuffer image对象V平面的缓冲
     * @param vRowStride Int V平面行步进
     * @param uPixelStride Int U或V平面像素步进
     * @param outData ByteArray 输出数据
     */
    external fun android420ToABGR(
        width: Int,
        height: Int,
        yPlane: ByteBuffer,
        yRowStride: Int,
        uPlane: ByteBuffer,
        uRowStride: Int,
        vPlane: ByteBuffer,
        vRowStride: Int,
        uPixelStride: Int,
        outData: ByteArray
    )

    /**
     * android420做变化（转为I420，做镜像，转为ABGR(对应android的ARGB)）
     * @param width Int 宽
     * @param height Int 高
     * @param yPlane ByteBuffer image对象Y平面的缓冲
     * @param yRowStride Int Y平面行步进
     * @param uPlane ByteBuffer image对象U平面的缓冲
     * @param uRowStride Int U平面行步进
     * @param vPlane ByteBuffer image对象V平面的缓冲
     * @param vRowStride Int V平面行步进
     * @param uPixelStride Int U或V平面像素步进
     * @param outData ByteArray 输出数据
     */
    external fun android420Mirror(
        width: Int,
        height: Int,
        yPlane: ByteBuffer,
        yRowStride: Int,
        uPlane: ByteBuffer,
        uRowStride: Int,
        vPlane: ByteBuffer,
        vRowStride: Int,
        uPixelStride: Int,
        outData: ByteArray,
    )

    /**
     * android420做变化（转为I420，做镜像，转为ABGR(对应android的ARGB)）
     * @param width Int 宽
     * @param height Int 高
     * @param yPlane ByteBuffer image对象Y平面的缓冲
     * @param yRowStride Int Y平面行步进
     * @param uPlane ByteBuffer image对象U平面的缓冲
     * @param uRowStride Int U平面行步进
     * @param vPlane ByteBuffer image对象V平面的缓冲
     * @param vRowStride Int V平面行步进
     * @param uPixelStride Int U或V平面像素步进
     * @param outData ByteArray 输出数据
     * @param degrees Int 旋转角度
     */
    external fun android420Rotate(
        width: Int,
        height: Int,
        yPlane: ByteBuffer,
        yRowStride: Int,
        uPlane: ByteBuffer,
        uRowStride: Int,
        vPlane: ByteBuffer,
        vRowStride: Int,
        uPixelStride: Int,
        outData: ByteArray,
        degrees: Int
    )
}
```
3. 创建`libyuv.cpp`文件用来调用libyuv库
```cpp
#include <jni.h>
#include <string>
#include "libyuv/version.h"
#include "libyuv/convert.h"

extern "C"
JNIEXPORT jint JNICALL
Java_com_roc_libyuv_YuvUtils_libYuvVersion(JNIEnv *env, jobject thiz) {
    return LIBYUV_VERSION;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_roc_libyuv_YuvUtils_android420ToABGR(JNIEnv *env, jobject thiz, jint width, jint height,
                                              jobject y_plane, jint y_row_stride, jobject u_plane,
                                              jint u_row_stride, jobject v_plane, jint v_row_stride,
                                              jint u_pixel_stride, jbyteArray out_data) {

    auto *pYData = (uint8_t *) env->GetDirectBufferAddress(y_plane);
    auto *pUData = (uint8_t *) env->GetDirectBufferAddress(u_plane);
    auto *pVData = (uint8_t *) env->GetDirectBufferAddress(v_plane);
    auto *pOutData = (uint8_t *) env->GetByteArrayElements(out_data, nullptr);

    libyuv::Android420ToABGR(
            pYData, y_row_stride,
            pUData, u_row_stride,
            pVData, v_row_stride,
            u_pixel_stride,
            pOutData, width * 4,
            width,
            height
    );
}

extern "C"
JNIEXPORT void JNICALL
Java_com_roc_libyuv_YuvUtils_android420Mirror(JNIEnv *env, jobject thiz, jint width, jint height,
                                              jobject y_plane, jint y_row_stride, jobject u_plane,
                                              jint u_row_stride, jobject v_plane, jint v_row_stride,
                                              jint u_pixel_stride, jbyteArray out_data) {
    //获取image的plane对应YUV层面数据
    auto *pYData = (uint8_t *) env->GetDirectBufferAddress(y_plane);
    auto *pUData = (uint8_t *) env->GetDirectBufferAddress(u_plane);
    auto *pVData = (uint8_t *) env->GetDirectBufferAddress(v_plane);
    auto *pOutData = (uint8_t *) env->GetByteArrayElements(out_data, nullptr);

    auto length = sizeof(uint8_t) * width * height * 3 / 2;
    int stride_y = width;
    int stride_uv = width >> 1;

    auto *src_i420_data = (uint8_t *) malloc(length);
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);
    uint8_t *i420_y = src_i420_data;
    uint8_t *i420_u = src_i420_data + src_y_size;
    uint8_t *i420_v = src_i420_data + src_y_size + src_u_size;

    libyuv::Android420ToI420(
            pYData, y_row_stride,
            pUData, u_row_stride,
            pVData, v_row_stride,
            u_pixel_stride,
            i420_y, stride_y,
            i420_u, stride_uv,
            i420_v, stride_uv,
            width,
            height
    );


    auto *src_i420_data2 = (uint8_t *) malloc(length);
    uint8_t *mirror_y = src_i420_data2;
    uint8_t *mirror_u = src_i420_data2 + src_y_size;
    uint8_t *mirror_v = src_i420_data2 + src_y_size + src_u_size;

    libyuv::I420Mirror(
            i420_y, stride_y,
            i420_u, stride_uv,
            i420_v, stride_uv,
            mirror_y, stride_y,
            mirror_u, stride_uv,
            mirror_v, stride_uv,
            width,
            height
    );

    libyuv::I420ToABGR(
            mirror_y, stride_y,
            mirror_u, stride_uv,
            mirror_v, stride_uv,
            pOutData,
            width * 4,
            width,
            height
    );

    free(src_i420_data);
    free(src_i420_data2);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_roc_libyuv_YuvUtils_android420Rotate(JNIEnv *env, jobject thiz, jint width, jint height,
                                              jobject y_plane, jint y_row_stride, jobject u_plane,
                                              jint u_row_stride, jobject v_plane, jint v_row_stride,
                                              jint u_pixel_stride, jbyteArray out_data,
                                              jint degrees) {
    //获取image的plane对应YUV层面数据
    auto *pYData = (uint8_t *) env->GetDirectBufferAddress(y_plane);
    auto *pUData = (uint8_t *) env->GetDirectBufferAddress(u_plane);
    auto *pVData = (uint8_t *) env->GetDirectBufferAddress(v_plane);
    auto *pOutData = (uint8_t *) env->GetByteArrayElements(out_data, nullptr);


    int i420_stride_y;
    int dst_width;
    int dst_height;
    switch (degrees) {
        case 90:
        case 270:
            i420_stride_y = height;
            dst_width = height;
            dst_height = width;
            break;
        case 180:
        default:
            i420_stride_y = width;
            dst_width = width;
            dst_height = height;
            break;
    }
    auto length = sizeof(uint8_t) * width * height * 3 / 2;
    int stride_y = i420_stride_y;
    int stride_uv = i420_stride_y >> 1;

    auto *src_i420_data = (uint8_t *) malloc(length);
    jint src_y_size = width * height;
    jint src_u_size = (width >> 1) * (height >> 1);
    uint8_t *i420_y = src_i420_data;
    uint8_t *i420_u = src_i420_data + src_y_size;
    uint8_t *i420_v = src_i420_data + src_y_size + src_u_size;


    libyuv::Android420ToI420Rotate(
            pYData, y_row_stride,
            pUData, u_row_stride,
            pVData, v_row_stride,
            u_pixel_stride,
            i420_y, stride_y,
            i420_u, stride_uv,
            i420_v, stride_uv,
            width,
            height,
            libyuv::RotationMode(degrees)
    );


    libyuv::I420ToABGR(
            i420_y, stride_y,
            i420_u, stride_uv,
            i420_v, stride_uv,
            pOutData,
            width * 4,
            dst_width,
            dst_height
    );

    free(src_i420_data);
}
```
### 开始使用libyuv
使用`CameraX`做相机预览和捕获图像数据，`CameraX`的固定用法，具体可以参考代码，主要说下获取图像数据核心
1. 从`IageProxy`中获取设置采集的`ImageFormat.YUV_420_888`图像数据
```kotlin
imageAnalysis.setAnalyzer(singleThreadPool) { imageProxy ->
    if (mTakeOneYuv.value) {
        when (imageProxy.format) {
            ImageFormat.YUV_420_888 -> {

                imageProxy.image?.apply {
                    //用来计算消耗时间
                    time = System.currentTimeMillis()

                    //创建用来接收处理后的ARGB数据
                    val outData = ByteArray(width * height * 4)
                    //android420做变化（转为I420，做镜像，转为ABGR(对应android的ARGB)）
                    YuvUtils.android420Rotate(
                        width = width,
                        height = height,
                        yPlane = planes[0].buffer,
                        yRowStride = planes[0].rowStride,
                        uPlane = planes[1].buffer,
                        uRowStride = planes[1].rowStride,
                        vPlane = planes[2].buffer,
                        vRowStride = planes[2].rowStride,
                        uPixelStride = planes[1].pixelStride,
                        outData = outData,
                        90
                    )
                    Log.e(
                        "消耗时间",
                        "${Thread.currentThread()}  android420Rotate - time = ${System.currentTimeMillis() - time}"
                    )
                    time = System.currentTimeMillis()
                    //将转换后的ARGB数据转换成Bitmap
                    val bitmapImageFromYUV =
                        CameraUtils.getBitmapImageFromYUV2(
                            data = outData,
                            width = width,
                            height = height
                        )
                    Log.e(
                        "消耗时间",
                        "${Thread.currentThread()}  getBitmapImageFromYUV2 - time = ${System.currentTimeMillis() - time}"
                    )
                    //更新Image刷新数据
                    imageBitmap.value = bitmapImageFromYUV
                }
            }

            else -> {//其他格式图像

            }
        }
    }
    imageProxy.close()
}
```
关于YUV格式不清楚的可以自己参考其他文章了解，对于其他格式的YUV数据获取自行实现。
2. 预览和转换效果
![GIF 2024-8-5 17-48-35](https://github.com/user-attachments/assets/b129e5a1-e7ad-404a-826e-595009794118)

### 最后附上Demo项目地址
https://github.com/Roc-egg/libyuv-android-demo
