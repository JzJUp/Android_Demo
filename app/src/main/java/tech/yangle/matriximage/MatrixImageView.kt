package tech.yangle.matriximage

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MASK
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import tech.yangle.matriximage.utils.MatrixImageUtils
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_1
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_2
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_3
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_4
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_5
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_6
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_CONTROL_7
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_IMAGE
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_OUTSIDE
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.TOUCH_ROTATE
import tech.yangle.matriximage.utils.MatrixImageUtils.callRotation
import tech.yangle.matriximage.utils.MatrixImageUtils.getDistanceOf2Points
import tech.yangle.matriximage.utils.MatrixImageUtils.getImageRectF
import tech.yangle.matriximage.utils.MatrixImageUtils.getMidPoint
import tech.yangle.matriximage.utils.MatrixImageUtils.getTouchMode
import tech.yangle.matriximage.utils.coroutineDelay
import kotlin.math.abs

/**
 * 支持移动、缩放、旋转功能的ImageView
 * <p>
 */
class MatrixImageView : AppCompatImageView {

    // 控件宽度
    private var mWidth = 0

    // 控件高度
    private var mHeight = 0

    // 第一次绘制
    private var mFirstDraw = true

    // 是否显示控制框
    private var mShowFrame = false

    // 当前Image矩阵
    /*平移 (postTranslate)：按指定的x和y值移动图像。
    mImgMatrix.postTranslate(tx, ty)
    缩放 (postScale)：按指定的x和y比例缩放图像。
    mImgMatrix.postScale(scaleX, scaleY)
    旋转 (postRotate)：绕指定的旋转中心点旋转图像。
    mImgMatrix.postRotate(degrees, centerX, centerY)
    错切 (postSkew)：沿x或y轴错切图像。
    mImgMatrix.postSkew(skewX, skewY)*/

    //Matrix 类是Android SDK中用于表示2D变换矩阵的类。
    // 这个矩阵可以用于执行各种图像变换，如平移（移动）、缩放、旋转和倾斜
    //可以使用 mImgMatrix 对象来设置各种变换，然后将其应用到 Canvas 或 Bitmap 对象上

    private var mImgMatrix = Matrix()

    // 画笔
    private lateinit var mPaint: Paint

    // 触摸模式
    private var touchMode: MatrixImageUtils.TouchMode? = null

    // 第二根手指是否按下
    private var mIsPointerDown = false

    // 按下点x坐标
    private var mDownX = 0f

    // 按下点y坐标
    private var mDownY = 0f

    // 上一次的触摸点x坐标
    private var mLastX = 0f

    // 上一次的触摸点y坐标
    private var mLastY = 0f

    // 上次双指触摸的 x1 的 x 坐标
    private var mLastDoubleTouchX1 = 0f

    // 上次双指触摸的 x1 的 y 坐标
    private var mLastDoubleTouchY1 = 0f

    // 上次双指触摸的 x2 的 x 坐标
    private var mLastDoubleTouchX2 = 0f

    // 上次双指触摸的 x2 的 7 坐标
    private var mLastDoubleTouchY2 = 0f

    private val scaleFactorMax = 1.0371976f
    private val scaleFactorMin = 0.96650776f


    // 旋转角度
    private var mDegree: Float = 0.0f

    // 旋转图标
    private lateinit var mRotateIcon: Bitmap

    // 图片控制框颜色
    private var mFrameColor = Color.parseColor("#1677FF")

    // 连接线宽度
    private var mLineWidth = dp2px(context, 2f)

    // 缩放控制点半径
    var mScaleDotRadius = dp2px(context, 5f)

    // 旋转控制点半径
    var mRotateDotRadius = dp2px(context, 12f)

    // 按下监听
    private var mDownClickListener: ((view: View, pointF: PointF) -> Unit)? = null

    // 长按监听
    private var mLongClickListener: ((view: View, pointF: PointF) -> Unit)? = null

    // 移动监听
    private var mMoveListener: ((view: View, pointF: PointF) -> Unit)? = null

    // 长按监听计时任务
    private var mLongClickJob: Job? = null

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        setAttribute(attrs)
        init()
    }

    private fun setAttribute(attrs: AttributeSet?) {
        if (attrs == null) {
            return
        }
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.MatrixImageView)
        val indexCount = typedArray.indexCount
        for (i in 0 until indexCount) {
            when (val attr = typedArray.getIndex(i)) {
                R.styleable.MatrixImageView_fcLineWidth -> { // 连接线宽度
                    mLineWidth = typedArray.getDimension(attr, mLineWidth)
                }

                R.styleable.MatrixImageView_fcScaleDotRadius -> { // 缩放控制点半径
                    mScaleDotRadius = typedArray.getDimension(attr, mScaleDotRadius)
                }

                R.styleable.MatrixImageView_fcRotateDotRadius -> { // 旋转控制点半径
                    mRotateDotRadius = typedArray.getDimension(attr, mRotateDotRadius)
                }

                R.styleable.MatrixImageView_fcFrameColor -> { // 图片控制框颜色
                    mFrameColor = typedArray.getColor(attr, mFrameColor)
                }
            }
        }
        typedArray.recycle()
    }

    private fun init() {
        mPaint = Paint().apply {
            isAntiAlias = true
            strokeWidth = mLineWidth
            color = mFrameColor
            style = Paint.Style.FILL
        }
        // Matrix模式
        //指定ImageView中图片的缩放类型
        scaleType = ScaleType.MATRIX

        // 旋转图标
        // 解码图片资源,旋转图标
        val rotateIcon = BitmapFactory.decodeResource(resources, R.mipmap.ic_mi_rotate)
        //计算一个基于 mRotateDotRadius 值的新宽度，该宽度用于后续的图像缩放。
        // 这里乘以1.6f是为了确保缩放后的图标大小符合设计需求
        val rotateIconWidth = (mRotateDotRadius * 1.6f).toInt()

        mRotateIcon = createScaledBitmap(rotateIcon, rotateIconWidth, rotateIconWidth, true)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        this.mWidth = w
        this.mHeight = h
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawable == null) {
            return
        }

        val imgRect = getImageRectF(this)
        // 左上角x坐标
        val left = imgRect.left
        // 左上角y坐标
        val top = imgRect.top
        // 右下角x坐标
        val right = imgRect.right
        // 右下角y坐标
        val bottom = imgRect.bottom

        // 图片移动到控件中心
        if (mFirstDraw) {
            mFirstDraw = false
            val centerX = (mWidth / 2).toFloat()
            val centerY = (mHeight / 2).toFloat()
            val imageWidth = right - left
            val imageHeight = bottom - top
            mImgMatrix.postTranslate(centerX - imageWidth / 2, centerY - imageHeight / 2)
            // 如果图片较大，缩放0.5倍
            if (imageWidth > width || imageHeight > height) {
                mImgMatrix.postScale(0.5f, 0.5f, centerX, centerY)
            }
            imageMatrix = mImgMatrix
        }

        // 不绘制控制框
        if (!mShowFrame) {
            return
        }
//        --------------------------------------------------------------------------------------------

//        val scaleMatrix = Matrix(imageMatrix)
//
//        val scaleRectF = getImageRectF(this, scaleMatrix)
//
//        val new_left = scaleRectF.left
//        // 左上角y坐标
//        val new_top = scaleRectF.top
//        // 右下角x坐标
//        val new_right = scaleRectF.right
//        // 右下角y坐标
//        val new_bottom = scaleRectF.bottom

//        // 上边框
//        canvas.drawLine(new_left, new_top, new_right, new_top, mPaint)
//        // 下边框
//        canvas.drawLine(new_left, new_bottom, new_right, new_bottom, mPaint)
//        // 左边框
//        canvas.drawLine(new_left, new_top, new_left, new_bottom, mPaint)
//        // 右边框
//        canvas.drawLine(new_right, new_top, new_right, new_bottom, mPaint)


//        --------------------------------------------------------------------------------------------

        // 上边框
        canvas.drawLine(left, top, right, top, mPaint)
        // 下边框
        canvas.drawLine(left, bottom, right, bottom, mPaint)
        // 左边框
        canvas.drawLine(left, top, left, bottom, mPaint)
        // 右边框
        canvas.drawLine(right, top, right, bottom, mPaint)

        // 左上角控制点，等比缩放
        canvas.drawCircle(left, top, mScaleDotRadius, mPaint)
        // 左下角控制点，等比缩放
        canvas.drawCircle(left, bottom, mScaleDotRadius, mPaint)
        // 右上角控制点，等比缩放
        canvas.drawCircle(right, top, mScaleDotRadius, mPaint)
        // 右下角控制点，等比缩放
        canvas.drawCircle(right, bottom, mScaleDotRadius, mPaint)

        // 左中间控制点，横向缩放
        canvas.drawCircle(left, top + (bottom - top) / 2, mScaleDotRadius, mPaint)
        // 右中间控制点，横向缩放
        canvas.drawCircle(right, top + (bottom - top) / 2, mScaleDotRadius, mPaint)

        // 下中间控制点，竖向缩放
        val middleX = (right - left) / 2 + left
        canvas.drawCircle(middleX, bottom, mScaleDotRadius, mPaint)
        // 上中间控制点，旋转
        val rotateLine = mRotateDotRadius / 3
        canvas.drawLine(middleX, top - rotateLine, middleX, top, mPaint)
        canvas.drawCircle(middleX, top - rotateLine - mRotateDotRadius, mRotateDotRadius, mPaint)
        // 上中间控制点，旋转图标
        canvas.drawBitmap(
            mRotateIcon,
            middleX - mRotateIcon.width / 2,
            top - rotateLine - mRotateDotRadius - mRotateIcon.width / 2,
            mPaint
        )
    }

    @SuppressLint("ClickableViewAccessibility", "SuspiciousIndentation")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null || drawable == null) {
            return super.onTouchEvent(event)
        }
        // x坐标
        val x = event.x
        // y坐标
        val y = event.y
        // 调用自定义函数 getImageRectF 来获取当前 View 的图像显示区域
        val imageRect = getImageRectF(this)
        // 图片中心点坐标x值
        val centerX = (imageRect.right - imageRect.left) / 2 + imageRect.left
        // 图片中心点坐标y值
        val centerY = (imageRect.bottom - imageRect.top) / 2 + imageRect.top

        //ACTION_MASK：是一个整数值，用于与 action 属性进行位与操作（bitwise AND operation），
        // 以获取主要的动作类型。这是因为从API level 5（Android 2.0）开始，
        // action 属性可能还包含其他信息，如 `多点触控` 中的指别号。
        when (event.action.and(ACTION_MASK)) {
            ACTION_DOWN -> {
                // 按下监听
                mDownClickListener?.invoke(this, PointF(x, y))
                // 判断是否在图片实际显示区域内
                touchMode = getTouchMode(this, x, y)
                if (touchMode == TOUCH_OUTSIDE) {
                    mShowFrame = false
                    invalidate()
//                    return super.onTouchEvent(event)
                    return true
                }

                //按下点x,y坐标
                mDownX = x
                mDownY = y
                //上一次的触摸点x,y坐标
                mLastX = x
                mLastY = y
                // 旋转控制点，点击后以图片中心为基准，计算当前旋转角度
                if (touchMode == TOUCH_ROTATE) {
                    // 记录旋转角度
                    mDegree = callRotation(centerX, centerY, x, y)
                }
                mShowFrame = true

//                Log.e("TAG","mDownX -> $mDownX  mLastX ->${mLastX}")
                invalidate()

                // 长按监听计时
                //调用 `coroutineDelay` 函数来初始化一个协程
                //这个 `coroutineDelay` 函数是一个通用的工具函数，可以用于在协程中实现各种需要延迟执行的场景
                mLongClickJob = coroutineDelay(Main, 500) {
                    //计算了自长按开始以来，当前触摸点与初始触摸点之间的水平和垂直偏移量
                    val offsetX = abs(x - mLastX)
                    val offsetY = abs(y - mLastY)

                    Log.e("TAG", "x -> $x  mLastX -> $mLastX  y-> $y  mLastY->$mLastY")
                    Log.e("TAG", "offsetX -> $offsetX  offsetY -> $offsetY")

                    //将 dp（密度无关像素）单位转换为像素单位
                    val offset = dp2px(context, 10f)
                    //如果计算出的偏移量在 10dp 以内，则认为这是一个长按事件，
                    // 然后调用 mLongClickListener 长按监听回调函数
                    if (offsetX <= offset && offsetY <= offset) {
                        mLongClickListener?.invoke(this, PointF(x, y))
                    }
                }
                return true
            }

            ACTION_CANCEL -> {
                mLongClickJob?.cancel()
            }

            //当用户将第二个或更多的手指放到触摸屏上时，会触发 ACTION_POINTER_DOWN 事件
            ACTION_POINTER_DOWN -> {
                mLongClickJob?.cancel()
                mDegree = callRotation(event)
                // 更新最后触摸点的坐标
                mLastDoubleTouchX1 = event.getX(0)
                mLastDoubleTouchY1 = event.getY(0)
                mLastDoubleTouchX2 = event.getX(1)
                mLastDoubleTouchY2 = event.getY(1)
                mIsPointerDown = true
                return true
            }

            ACTION_MOVE -> {
                // 旋转事件
                //当两个指针同时接触屏幕时，进入这个条件块
                if (event.pointerCount == 2) {
                    if (!mIsPointerDown) {
                        return true
                    }

                    val LastPointsOfDistance = getDistanceOf2Points(
                        mLastDoubleTouchX1,
                        mLastDoubleTouchY1,
                        mLastDoubleTouchX2,
                        mLastDoubleTouchY2
                    )
                    val nowPointsOfDistance = getDistanceOf2Points(event)

                    Log.e(
                        "Tag",
                        "LastPointsOfDistance -> $LastPointsOfDistance  pointsOfDistance -> $nowPointsOfDistance"
                    )
                    Log.e("Tag", "Distance -> ${nowPointsOfDistance - LastPointsOfDistance}")

                    val scaleFactor = (nowPointsOfDistance / LastPointsOfDistance)
//                    if (scaleFactor > scaleFactorMax ){
//                        scaleFactor = scaleFactorMax
//                    }else if(scaleFactor<scaleFactorMin){
//                        scaleFactor = scaleFactorMin
//                    }
                    Log.e("TAG", "双指缩放因子 -> $scaleFactor")

                    val scaleMatrix = Matrix(mImgMatrix)
                    scaleMatrix.postScale(scaleFactor, scaleFactor, centerX, centerY)
                    val scaleRectF = getImageRectF(this, scaleMatrix)
                    if (scaleRectF.right - scaleRectF.left < mScaleDotRadius * 6 || scaleRectF.bottom - scaleRectF.top < mScaleDotRadius * 6) {
                        return true
                    }
                    if (scaleRectF.right - scaleRectF.left > mScaleDotRadius * 300 || scaleRectF.bottom - scaleRectF.top > mScaleDotRadius * 300) {
                        return true
                    }

                    mImgMatrix.postScale(scaleFactor, scaleFactor, centerX, centerY)
                    imageMatrix = scaleMatrix


                    //计算两根手指旋转的角度
                    val rotate = callRotation(event)
                    //计算自上次更新以来旋转角度的变化量
                    val rotateNow = rotate - mDegree
                    //更新当前的旋转角度为最新计算的角度
                    mDegree = rotate
                    //在 mImgMatrix（图像矩阵）上应用旋转变换，以 (centerX, centerY) 为中心点。
                    mImgMatrix.postRotate(rotateNow, centerX, centerY)
                    //将更新后的矩阵赋值给 imageMatrix
                    imageMatrix = mImgMatrix

                    // 更新最后触摸点的坐标
                    mLastDoubleTouchX1 = event.getX(0)
                    mLastDoubleTouchY1 = event.getY(0)
                    mLastDoubleTouchX2 = event.getX(1)
                    mLastDoubleTouchY2 = event.getY(1)
                    return true
                }
                if (mIsPointerDown) {
                    return true
                }
                // 单指操作的移动、缩放事件
                touchMove(x, y, imageRect)
                mLastX = x
                mLastY = y
                invalidate()

                //计算当前触摸点与初始触摸点之间的水平和垂直偏移量
                val offsetX = abs(x - mDownX)
                val offsetY = abs(y - mDownY)
                val offset = dp2px(context, 10f)
                //如果移动距离超过一定阈值，则调用 mMoveListener 移动监听器
                if (offsetX > offset || offsetY > offset) {
                    mMoveListener?.invoke(this, PointF(x, y))
                }
                return true
            }

            ACTION_UP -> {
                mLongClickJob?.cancel()
                touchMode = null
                mIsPointerDown = false
                mDegree = 0f
            }
        }
//        return super.onTouchEvent(event)
        return true
    }

    /**
     * 手指移动
     *
     * @param x         x坐标
     * @param y         y坐标
     * @param imageRect 图片显示区域
     */

    private fun touchMove(x: Float, y: Float, imageRect: RectF) {
        // 左上角x坐标
        val left = imageRect.left
        // 左上角y坐标
        val top = imageRect.top
        // 右下角x坐标
        val right = imageRect.right
        // 右下角y坐标
        val bottom = imageRect.bottom

        // 总的缩放距离，对角线距离
        val totalTransOblique = getDistanceOf2Points(left, top, right, bottom)
        // 总的缩放距离，水平距离
        val totalTransHorizontal = getDistanceOf2Points(left, top, right, top)
        // 总的缩放距离，垂直距离
        val totalTransVertical = getDistanceOf2Points(left, top, left, bottom)
        // 当前缩放距离
        val scaleTrans = getDistanceOf2Points(mLastX, mLastY, x, y)

        // 缩放系数，x轴方向
        val scaleFactorX: Float
        // 缩放系数，y轴方向
        val scaleFactorY: Float
        // 缩放基准点x坐标
        val scaleBaseX: Float
        // 缩放基准点y坐标
        val scaleBaseY: Float

        when (touchMode) {
            //图片显示区域
            TOUCH_IMAGE -> {
                mImgMatrix.postTranslate(x - mLastX, y - mLastY)
                imageMatrix = mImgMatrix
                return
            }

            //旋转控制点
            TOUCH_ROTATE -> {
                // 图片中心点x坐标
                val centerX = (imageRect.right - imageRect.left) / 2 + imageRect.left
                // 图片中心点y坐标
                val centerY = (imageRect.bottom - imageRect.top) / 2 + imageRect.top
                // mDegree旋转角度
                val rotate = callRotation(centerX, centerY, x, y)
                val rotateNow = rotate - mDegree
                //记录当前旋转角度，上次更新是在 `TOUCH_DOWN` 事件里
                mDegree = rotate
                mImgMatrix.postRotate(rotateNow, centerX, centerY)
                //将旋转后的矩阵赋值给imageMatrix变量，用于绘制
                imageMatrix = mImgMatrix
                return
            }

            //左上角控制点，等比缩放
            TOUCH_CONTROL_1 -> {
                //scaleFactor是`缩放系数`
                //左上角往左滑动是放大，在当前x值比之前触摸点的x小就是放大，反之是缩小
                scaleFactorX = if (x - mLastX > 0) {
                    //缩小
                    (totalTransOblique - scaleTrans) / totalTransOblique
                } else {
                    //增大
                    (totalTransOblique + scaleTrans) / totalTransOblique
                }
                Log.e(
                    "Tag",
                    "对角线距离 -> $totalTransOblique 当前缩放距离-> $scaleTrans  scaleFactorX -> $scaleFactorX"
                )
                // 等比例放大 -> 所以Y的缩放系数和X的缩放系数相等
                scaleFactorY = scaleFactorX
                // 右下角
                scaleBaseX = imageRect.right
                scaleBaseY = imageRect.bottom

            }

            //右上角控制点，等比缩放
            TOUCH_CONTROL_2 -> {
                scaleFactorX = if (x - mLastX < 0) {
                    // 缩小
                    (totalTransOblique - scaleTrans) / totalTransOblique
                } else {
                    //放大
                    (totalTransOblique + scaleTrans) / totalTransOblique
                }

                scaleFactorY = scaleFactorX
                // 左下角
                scaleBaseX = imageRect.left
                scaleBaseY = imageRect.bottom
            }

            //左下角控制点，等比缩放
            TOUCH_CONTROL_3 -> {
                // 缩小
                scaleFactorX = if (x - mLastX > 0) {
                    (totalTransOblique - scaleTrans) / totalTransOblique
                } else {
                    (totalTransOblique + scaleTrans) / totalTransOblique
                }
                scaleFactorY = scaleFactorX
                // 右上角
                scaleBaseX = imageRect.right
                scaleBaseY = imageRect.top
            }
            //右下角控制点，等比缩放
            TOUCH_CONTROL_4 -> {
                // 缩小
                scaleFactorX = if (x - mLastX < 0) {
                    (totalTransOblique - scaleTrans) / totalTransOblique
                } else {
                    (totalTransOblique + scaleTrans) / totalTransOblique
                }
                scaleFactorY = scaleFactorX
                // 左上角
                scaleBaseX = imageRect.left
                scaleBaseY = imageRect.top
            }

            //左中间控制点，横向缩放
            TOUCH_CONTROL_5 -> {
                scaleFactorX = if (x - mLastX > 0) {
                    //缩小
                    (totalTransHorizontal - scaleTrans) / totalTransHorizontal
                } else {
                    //放大
                    (totalTransHorizontal + scaleTrans) / totalTransHorizontal
                }
                //设置纵向缩放因子
                scaleFactorY = 1f

                // 右上角
                scaleBaseX = imageRect.right
                scaleBaseY = imageRect.top
            }
            //右中间控制点，横向缩放
            TOUCH_CONTROL_6 -> {
                // 缩小
                scaleFactorX = if (x - mLastX < 0) {
                    (totalTransHorizontal - scaleTrans) / totalTransHorizontal
                } else {
                    (totalTransHorizontal + scaleTrans) / totalTransHorizontal
                }
                scaleFactorY = 1f
                // 左上角
                scaleBaseX = imageRect.left
                scaleBaseY = imageRect.top
            }

            //下中间控制点，竖向缩放
            TOUCH_CONTROL_7 -> {
                scaleFactorX = 1f
                scaleFactorY = if (y - mLastY < 0) {
                    // 缩小
                    (totalTransVertical - scaleTrans) / totalTransVertical
                } else {
                    //放大
                    (totalTransVertical + scaleTrans) / totalTransVertical
                }
                // 左上角
                scaleBaseX = imageRect.left
                scaleBaseY = imageRect.top
            }

            else -> {
                return
            }
        }

        // 最小缩放值限制
        //创建一个新的 Matrix 对象 scaleMatrix，它是 mImgMatrix 的一个副本
        // 可以在 scaleMatrix 上执行操作而不改变原始的 mImgMatrix
        val scaleMatrix = Matrix(mImgMatrix)
        scaleMatrix.postScale(scaleFactorX, scaleFactorY, scaleBaseX, scaleBaseY)
        val scaleRectF = getImageRectF(this, scaleMatrix)
        if (scaleRectF.right - scaleRectF.left < mScaleDotRadius * 6
            || scaleRectF.bottom - scaleRectF.top < mScaleDotRadius * 6
        ) {
            return
        }
        // 缩放
        mImgMatrix.postScale(scaleFactorX, scaleFactorY, scaleBaseX, scaleBaseY)
        imageMatrix = mImgMatrix
    }


    /**
     * 隐藏控制框
     */

    fun hideControlFrame() {
        mShowFrame = false
        invalidate()
    }

    /**
     * 设置按下监听
     *
     * @param listener 监听回调
     */
    fun setOnImageDownClickListener(listener: (view: View, pointF: PointF) -> Unit) {
        this.mDownClickListener = listener
    }

    /**
     * 设置长按监听
     *
     * @param listener 监听回调
     */
    fun setOnImageLongClickListener(listener: (view: View, pointF: PointF) -> Unit) {
        this.mLongClickListener = listener
    }

    /**
     * 设置移动监听
     *
     * @param listener 监听回调
     */
    fun setOnImageMoveListener(listener: (view: View, pointF: PointF) -> Unit) {
        this.mMoveListener = listener
    }

    /**
     * dp 转 px
     *
     * @param context 上下文
     * @param dp      dp单位值
     * @return px单位制
     */
    private fun dp2px(context: Context, dp: Float): Float {
        return TypedValue.applyDimension(COMPLEX_UNIT_DIP, dp, context.resources.displayMetrics)
    }
}