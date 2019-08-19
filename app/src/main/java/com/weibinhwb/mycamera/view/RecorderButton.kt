package com.weibinhwb.mycamera.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.weibinhwb.mycamera.R
import kotlin.math.min

/**
 * Created by weibin on 2019/8/18
 */


class RecorderButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mSPaint: Paint? = null
    private var mBPaint: Paint? = null

    private var mCenterX: Int = 0

    private var mCenterY: Int = 0

    private val mRadius: Int

    private val mGap = 20

    init {
        val typedArray = context.obtainStyledAttributes(R.styleable.RecorderButton)
        mRadius = typedArray.getInt(R.styleable.RecorderButton_record_radius, 40)
        typedArray.recycle()
        init()
    }

    private fun init() {
        mBPaint = Paint()
        mBPaint!!.color = -0x11232324
        mBPaint!!.style = Paint.Style.FILL
        mBPaint!!.isAntiAlias = true

        mSPaint = Paint()
        mSPaint!!.color = -0x1
        mSPaint!!.style = Paint.Style.FILL
        mSPaint!!.isAntiAlias = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCenterX = w / 2
        mCenterY = h / 2
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)
        Log.d("weibin", "$widthSize   -widthSize-")
        Log.d("weibin", "$heightSize  -heightSize-")

        var size = min(widthSize, heightSize)
        if (widthMode == View.MeasureSpec.AT_MOST || heightMode == View.MeasureSpec.AT_MOST) {
            Log.d("weibin", "yes")
            size = 2 * (2 * mGap + mRadius)
        }
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(mCenterX.toFloat(), mCenterY.toFloat(), (mRadius + mGap).toFloat(), mBPaint!!)
        canvas.drawCircle(mCenterX.toFloat(), mCenterY.toFloat(), mRadius.toFloat(), mSPaint!!)
    }

}
