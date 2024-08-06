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