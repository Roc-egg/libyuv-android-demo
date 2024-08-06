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