package com.roc.ndkexample.utils

import android.R.attr
import android.R.attr.width
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.media.Image.Plane
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer


/**
 *
 * @Author Roc
 * @Date   2024/7/2
 * @Name   CameraUtils
 */
object CameraUtils {

    //Planar格式（P）的处理
    private fun getUvBufferWithoutPaddingP(
        uBuffer: ByteBuffer,
        vBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int
    ): ByteBuffer {
        var pos = 0
        val byteArray = ByteArray(height * width / 2)
        for (row in 0 until height / 2) {
            for (col in 0 until width / 2) {
                val vuPos = col * pixelStride + row * rowStride
                byteArray[pos++] = vBuffer[vuPos]
                byteArray[pos++] = uBuffer[vuPos]
            }
        }
        val bufferWithoutPaddings = ByteBuffer.allocate(byteArray.size)
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray)
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip()
        return bufferWithoutPaddings
    }

    //Semi-Planar格式（SP）的处理和y通道的数据
    private fun getBufferWithoutPadding(
        buffer: ByteBuffer,
        width: Int,
        rowStride: Int,
        times: Int,
        isVbuffer: Boolean
    ): ByteBuffer {
        var width = width
        if (width == rowStride) return buffer //没有buffer,不用处理。

        var bufferPos = buffer.position()
        val cap = buffer.capacity()
        val byteArray = ByteArray(times * width)
        var pos = 0
        //对于y平面，要逐行赋值的次数就是height次。对于uv交替的平面，赋值的次数是height/2次
        for (i in 0 until times) {
            buffer.position(bufferPos)
            //part 1.1 对于u,v通道,会缺失最后一个像u值或者v值，因此需要特殊处理，否则会crash
            if (isVbuffer && i == times - 1) {
                width = width - 1
            }
            buffer[byteArray, pos, width]
            bufferPos += rowStride
            pos = pos + width
        }

        //nv21数组转成buffer并返回
        val bufferWithoutPaddings = ByteBuffer.allocate(byteArray.size)
        // 数组放到buffer中
        bufferWithoutPaddings.put(byteArray)
        //重置 limit 和postion 值否则 buffer 读取数据不对
        bufferWithoutPaddings.flip()
        return bufferWithoutPaddings
    }

    fun YUV_420_888toNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val yBuffer = getBufferWithoutPadding(
            image.planes[0].buffer,
            image.width,
            image.planes[0].rowStride,
            image.height,
            false
        )
        //part1 获得真正的消除padding的ybuffer和ubuffer。需要对P格式和SP格式做不同的处理。如果是P格式的话只能逐像素去做，性能会降低。
        val vBuffer = if (image.planes[2].pixelStride == 1) { //如果为true，说明是P格式。
            getUvBufferWithoutPaddingP(
                image.planes[1].buffer, image.planes[2].buffer,
                width, height, image.planes[1].rowStride, image.planes[1].pixelStride
            )
        } else {
            getBufferWithoutPadding(
                image.planes[2].buffer,
                image.width,
                image.planes[2].rowStride,
                image.height / 2,
                true
            )
        }

        //part2 将y数据和uv的交替数据（除去最后一个v值）赋值给nv21
        val ySize = yBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21: ByteArray
        val byteSize = width * height * 3 / 2
        nv21 = ByteArray(byteSize)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]

        //part3 最后一个像素值的u值是缺失的，因此需要从u平面取一下。
        val uPlane = image.planes[1].buffer
        val lastValue = uPlane[uPlane.capacity() - 1]
        nv21[byteSize - 1] = lastValue
        return nv21
    }


    fun getBitmapImageFromYUV(data: ByteArray?, width: Int, height: Int): Bitmap {
        val yuvImage = YuvImage(data, ImageFormat.NV21, width, height, null)
        val outStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, width, height), 80, outStream)
        val outData = outStream.toByteArray()
        val bitmapFactoryOptions = BitmapFactory.Options()
        bitmapFactoryOptions.inPreferredConfig = Bitmap.Config.RGB_565
        val bmp = BitmapFactory.decodeByteArray(outData, 0, outData.size, bitmapFactoryOptions)
        return bmp
    }

    fun getBitmapImageFromYUV2(data: ByteArray, width: Int, height: Int): Bitmap {
        val buffer = ByteBuffer.wrap(data)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }


    fun yuv420ToNV21(image: Image): ByteArray {
        val w: Int = image.width
        val h: Int = image.height

        // size是宽乘高的1.5倍 可以通过ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888)得到
        val i420Size = w * h * 3 / 2

        val planes: Array<Plane> = image.getPlanes()

        //remaining0 = rowStride*(h-1)+w => 27632= 192*143+176 Y分量byte数组的size
        val remaining0 = planes[0].buffer.remaining()
        val remaining1 = planes[1].buffer.remaining()

        //remaining2 = rowStride*(h/2-1)+w-1 =>  13807=  192*71+176-1 V分量byte数组的size
        val remaining2 = planes[2].buffer.remaining()

        //获取pixelStride，可能跟width相等，可能不相等
        val pixelStride = planes[2].pixelStride
        val rowOffest = planes[2].rowStride
        val nv21 = ByteArray(i420Size)

        //分别准备三个数组接收YUV分量。
        val yRawSrcBytes = ByteArray(remaining0)
        val uRawSrcBytes = ByteArray(remaining1)
        val vRawSrcBytes = ByteArray(remaining2)
        planes[0].buffer[yRawSrcBytes]
        planes[1].buffer[uRawSrcBytes]
        planes[2].buffer[vRawSrcBytes]
        if (pixelStride == width) {
            //两者相等，说明每个YUV块紧密相连，可以直接拷贝
            System.arraycopy(yRawSrcBytes, 0, nv21, 0, rowOffest * h)
            System.arraycopy(vRawSrcBytes, 0, nv21, rowOffest * h, rowOffest * h / 2 - 1)
        } else {
            //根据每个分量的size先生成byte数组
            val ySrcBytes = ByteArray(w * h)
            val uSrcBytes = ByteArray(w * h / 2 - 1)
            val vSrcBytes = ByteArray(w * h / 2 - 1)
            for (row in 0 until h) {
                //源数组每隔 rowOffest 个bytes 拷贝 w 个bytes到目标数组
                System.arraycopy(yRawSrcBytes, rowOffest * row, ySrcBytes, w * row, w)
                //y执行两次，uv执行一次
                if (row % 2 == 0) {
                    //最后一行需要减一
                    if (row == h - 2) {
                        System.arraycopy(
                            vRawSrcBytes,
                            rowOffest * row / 2,
                            vSrcBytes,
                            w * row / 2,
                            w - 1
                        )
                    } else {
                        System.arraycopy(
                            vRawSrcBytes,
                            rowOffest * row / 2,
                            vSrcBytes,
                            w * row / 2,
                            w
                        )
                    }
                }
            }
            //yuv拷贝到一个数组里面
            System.arraycopy(ySrcBytes, 0, nv21, 0, w * h)
            System.arraycopy(vSrcBytes, 0, nv21, w * h, w * h / 2 - 1)
        }
        return nv21
    }


    /**从[Image]中计算出来实际宽高的帧数据*/
    private var newData: ByteArray? = null

    /**重新计算[Image]帧数据的Y帧行临时数据*/
    private var rowData: ByteArray? = null

    /**减少频繁创建上面两个[ByteArray]的判断参数*/
    private var imageWidth: Int? = null
    private var imageHigh: Int? = null
    private var imageRowStride: Int? = null

    fun imageToByteArray(image: Image): ByteArray? {

        /**获取[Image]宽高*/
        val width = image.width
        val height = image.height
        /**[PixelFormat.RGBA_8888]数据只有 Y帧 */
        if (image.planes.isNotEmpty()) {
            val buffer = image.planes[0].buffer
            val pixelStride = image.planes[0].pixelStride//像素之间间隔
            val rowStride = image.planes[0].rowStride//行与之间间隔，也就是行长度
//                        val rowPadding = rowStride - pixelStride * width//行尾填充=每行长度-像素间隔*图片宽度
//                        val planeWidth = width + rowPadding / pixelStride
//                        LogUtils.e(
//                            "测试数据",
//                            "planes数据值  pixelStride = $pixelStride , rowStride = $rowStride , rowPadding = $rowPadding , planeWidth = $planeWidth"
//                        )
//                        LogUtils.e("测试数据", "format = $format , width = $width , height = $height")
            /**创建一个没有填充的新数组用于存储重新计算真实宽高后裁剪的数据，如果宽高变更则重新初始化大小*/
            if (newData == null || imageWidth != width || imageHigh != height) {
                /**获取[Image]图像的宽高*/
                imageWidth = width
                imageHigh = height
                newData = ByteArray(width * height * pixelStride)
            }
            /**临时存储每行数据*/
            if (rowData == null || imageRowStride != rowStride) {
                imageRowStride = rowStride
                rowData = ByteArray(rowStride) // 临时存储每行数据
            }
            /**设置buffer缓冲区的位置*/
            buffer.position(0)
            for (i in 0 until height) {
                /**读取一行数据到rowData*/
                buffer[rowData!!, 0, rowStride]
                /**从rowData复制有效数据到data*/
                System.arraycopy(
                    rowData!!,
                    0,
                    newData!!,
                    i * width * pixelStride,
                    width * pixelStride
                )
            }
            return newData
        }
        return null
    }


    fun getRgbaData(image: Image): Bitmap {
        val planes: Array<Plane> = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding: Int = rowStride - pixelStride * image.width

        val bitmap =
            Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
        bitmap.copyPixelsFromBuffer(buffer)
        return bitmap
    }
}