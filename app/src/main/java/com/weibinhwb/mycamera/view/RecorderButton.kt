package com.weibinhwb.mycamera.view

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.weibinhwb.mycamera.R
import com.weibinhwb.mycamera.utils.LogUtil
import kotlin.math.min

/**
 * Created by weibin on 2019/8/18
 */


class RecorderButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val mSmallCirclePaint = Paint()
    private val mBigCirclePaint = Paint()
    private val mOutlinePaint = Paint()

    //圆心坐标x
    private var mCenterX: Int = 0
    //圆心坐标y
    private var mCenterY: Int = 0
    //最小的圆的
    private var mRadius: Int

    private val mGap = 30

    private val valueAnimator: ValueAnimator
    private var mDeltaRadius: Float = 0.0f

    private var isDown = false

    init {
        val typedArray = context.obtainStyledAttributes(R.styleable.RecorderButton)
        mRadius = typedArray.getInt(R.styleable.RecorderButton_record_radius, 40)
        typedArray.recycle()

        mBigCirclePaint.color = -0x11232324
        mBigCirclePaint.style = Paint.Style.FILL
        mBigCirclePaint.isAntiAlias = true

        mSmallCirclePaint.color = -0x1
        mSmallCirclePaint.style = Paint.Style.FILL
        mSmallCirclePaint.isAntiAlias = true

        mOutlinePaint.color = Color.GREEN
        mOutlinePaint.style = Paint.Style.STROKE
        mOutlinePaint.isAntiAlias = true

        valueAnimator = ValueAnimator.ofFloat(0.0f, mGap.toFloat())
        valueAnimator.duration = 2000
        valueAnimator.addUpdateListener {
            mDeltaRadius = it.animatedValue as Float
            LogUtil.d(this.javaClass.name, "radius = $mDeltaRadius")
            invalidate()
        }
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
        canvas.drawCircle(
            mCenterX.toFloat(),
            mCenterY.toFloat(),
            (mRadius + mGap).toFloat(),
            mBigCirclePaint
        )
        canvas.drawCircle(
            mCenterX.toFloat(),
            mCenterY.toFloat(),
            mRadius.toFloat() + mDeltaRadius,
            mSmallCirclePaint
        )
        if (isDown) {
            canvas.drawCircle(
                mCenterX.toFloat(),
                mCenterY.toFloat(),
                (mRadius + mGap).toFloat(),
                mOutlinePaint
            )
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                LogUtil.d(this.javaClass.name, "down")
                valueAnimator.start()
                mRadius += 20
                isDown = true
                callPress()
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                LogUtil.d(this.javaClass.name, "move")
            }
            MotionEvent.ACTION_UP -> {
                LogUtil.d(this.javaClass.name, "up")
                valueAnimator.pause()
                mDeltaRadius = 0.0f
                mRadius -= 20
                isDown = false
                callPress()
                invalidate()
            }
        }
        return true
    }

    private var mPressListener: OnPressListener? = null

    private fun callPress() {
        mPressListener?.press()
    }

    fun setOnPressListener(listener: OnPressListener) {
        mPressListener = listener
    }

    interface OnPressListener {
        fun press()
    }
}
