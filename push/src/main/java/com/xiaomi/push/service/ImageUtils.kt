package com.xiaomi.push.service

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetrics
import kotlin.math.max


object ImageUtils {
    fun textToBitmap(text: String, textSize: Float, bgColor: Int, textColor: Int): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.color = textColor
        paint.textAlign = Paint.Align.CENTER

        // 计算文本宽高
        val textWidth: Float = paint.measureText(text)
        val fm: FontMetrics = paint.getFontMetrics()
        val textHeight = fm.bottom - fm.top

        // 创建背景为正方形的Bitmap
        val size = max(textWidth.toDouble(), textHeight.toDouble()).toInt() + 20 // 增加边距
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(bgColor)

        // 居中绘制文本
        val x = size / 2f
        val y = (size - fm.bottom - fm.top) / 2f
        canvas.drawText(text, x, y, paint)

        return bitmap
    }
}