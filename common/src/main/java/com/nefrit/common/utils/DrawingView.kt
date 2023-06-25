package com.nefrit.common.utils

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View


//class MainActivity : Activity() {
//    var dv: MainActivity.DrawingView? = null
//    private var mPaint: Paint? = null
//    override fun onCreate(savedInstanceState: Bundle) {
//        super.onCreate(savedInstanceState)
//        dv = MainActivity.DrawingView(this)
//        setContentView(dv)
//        mPaint = Paint()
//        mPaint.setAntiAlias(true)
//        mPaint.setDither(true)
//        mPaint.setColor(Color.GREEN)
//        mPaint.setStyle(Paint.Style.STROKE)
//        mPaint.setStrokeJoin(Paint.Join.ROUND)
//        mPaint.setStrokeCap(Paint.Cap.ROUND)
//        mPaint.setStrokeWidth(12)
//    }

class DrawingView(val c: Context, val bitmap: Bitmap) : View(c) {
    val mPaint = Paint()

    private var mBitmap: Bitmap? = null
    private var mCanvas: Canvas? = null
    private val mPath: Path
    private val mBitmapPaint: Paint
    private val circlePaint: Paint
    private val circlePath: Path

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
//        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        mBitmap = this.bitmap
        mCanvas = Canvas(mBitmap)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mBitmap, 0f, 0f, mBitmapPaint)
        canvas.drawPath(mPath, mPaint)
        canvas.drawPath(circlePath, circlePaint)
    }

    private var mX = 0f
    private var mY = 0f

    init {
        mPath = Path()
        mBitmapPaint = Paint(Paint.DITHER_FLAG)
        circlePaint = Paint()
        circlePath = Path()
        circlePaint.isAntiAlias = true
        circlePaint.setColor(Color.BLUE)
        circlePaint.setStyle(Paint.Style.STROKE)
        circlePaint.setStrokeJoin(Paint.Join.MITER)
        circlePaint.setStrokeWidth(4f)
    }

    private fun touch_start(x: Float, y: Float) {
        mPath.reset()
        mPath.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touch_move(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
            mX = x
            mY = y
            circlePath.reset()
            circlePath.addCircle(mX, mY, 30F, Path.Direction.CW)
        }
    }

    private fun touch_up() {
        mPath.lineTo(mX, mY)
        circlePath.reset()
        // commit the path to our offscreen
        mCanvas?.drawPath(mPath, mPaint)
        // kill this so we don't double draw
        mPath.reset()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                touch_move(x, y)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
    }
}
