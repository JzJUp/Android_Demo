package tech.yangle.matriximage.utils

import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.ImageView
import tech.yangle.matriximage.MatrixImageView
import tech.yangle.matriximage.utils.MatrixImageUtils.TouchMode.*
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 图片工具
 * <p>
 * Created by yangle on 2021/8/10.
 * Website：http://www.yangle.tech
 */
object MatrixImageUtils {

    /**
     * 是否点击了图片的实际显示区域
     *
     * @param view ImageView
     * @param x    点击x坐标
     * @param y    点击y坐标
     * @return true: 区域内 false: 区域外
     */

    internal fun getTouchMode(view: MatrixImageView, x: Float, y: Float): TouchMode {
        val imageRect = getImageRectF(view)
        val left = imageRect.left
        val top = imageRect.top
        val right = imageRect.right
        val bottom = imageRect.bottom
        // 扩大点击区域
        //mScaleDotRadius是缩放控制点半径
        val offset = view.mScaleDotRadius * 3

        val rect1 = RectF(left - offset, top - offset, left + offset, top + offset)
        if (rect1.contains(x, y)) {
            return TOUCH_CONTROL_1
        }
        val rect2 = RectF(right - offset, top - offset, right + offset, top + offset)
        if (rect2.contains(x, y)) {
            return TOUCH_CONTROL_2
        }
        val rect3 = RectF(left - offset, bottom - offset, left + offset, bottom + offset)
        if (rect3.contains(x, y)) {
            return TOUCH_CONTROL_3
        }
        val rect4 = RectF(right - offset, bottom - offset, right + offset, bottom + offset)
        if (rect4.contains(x, y)) {
            return TOUCH_CONTROL_4
        }
        val rect5Y = (bottom - top) / 2 + top
        val rect5 = RectF(left - offset, rect5Y - offset, left + offset, rect5Y + offset)
        if (rect5.contains(x, y)) {
            return TOUCH_CONTROL_5
        }
        val rect6Y = (bottom - top) / 2 + top
        val rect6 = RectF(right - offset, rect6Y - offset, right + offset, rect6Y + offset)
        if (rect6.contains(x, y)) {
            return TOUCH_CONTROL_6
        }
        val rect7X = (right - left) / 2 + left
        val rect7 = RectF(rect7X - offset, bottom - offset, rect7X + offset, bottom + offset)
        if (rect7.contains(x, y)) {
            return TOUCH_CONTROL_7
        }
        // 旋转控制点半径
        val rotateDotRadius = view.mRotateDotRadius
        // 旋转控制点的中心x坐标
        val rectRotateX = (right - left) / 2 + left
        // rotateDotRadius / 3 是连接线的长度
        val rectRotate = RectF(
            rectRotateX - rotateDotRadius,
            top - rotateDotRadius / 3 - rotateDotRadius * 2,
            rectRotateX + rotateDotRadius,
            top - rotateDotRadius / 3,
        )
        if (rectRotate.contains(x, y)) {
            return TOUCH_ROTATE
        }
        if (imageRect.contains(x, y)) {
            return TOUCH_IMAGE
        }
        return TOUCH_OUTSIDE
    }

    /**
     * 获取图片在ImageView中的实际显示位置
     *
     * @param view ImageView
     * @return RectF
     */
    //两个函数都名为 getImageRectF，但具有不同的参数列表，（第二个函数重载了第一个函数）
    internal fun getImageRectF(view: ImageView): RectF {
        //从传入的 ImageView 对象中获取其 imageMatrix 属性。
        // imageMatrix 是一个 Matrix 类型的对象，表示当前图像的变换矩阵，包括平移、缩放、旋转等变换。
        val matrix = view.imageMatrix
        return getImageRectF(view, matrix)
    }

    // 获得ImageView中Image的显示边界
    internal fun getImageRectF(view: ImageView, matrix: Matrix): RectF {
        //获取 ImageView 中的 Drawable（图像）的显示边界。
        // bounds 是一个 Rect 对象，表示图像在 ImageView 中的位置和大小
        val bounds = view.drawable.bounds
        //用于存储变换后的边界
        val rectF = RectF()
        matrix.mapRect(
            rectF,
            RectF(
                bounds.left.toFloat(),
                bounds.top.toFloat(),
                bounds.right.toFloat(),
                bounds.bottom.toFloat()
            )
        )
        return rectF
    }

    /**
     * 获取两个点之间的距离
     *
     * @param x1 第一个点x坐标
     * @param y1 第一个点y坐标
     * @param x2 第二个点x坐标
     * @param y2 第二个点y坐标
     * @return 两个点之间的距离
     */

    internal fun getDistanceOf2Points(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }

    internal fun getDistanceOf2Points(event: MotionEvent): Float {
        //第一个点的坐标
        val x1:Float = event.getX(0)
        val y1:Float = event.getY(0)
        //第二个点的坐标
        val x2:Float = event.getX(1)
        val y2:Float = event.getY(1)

        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }

    /**
     * 计算旋转的角度
     *
     * @param event MotionEvent
     * @return 旋转的角度
     */
    //计算并返回两个点之间旋转的角度(基于图片的中心点)
    internal fun callRotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radius = atan2(deltaY, deltaX)
        return Math.toDegrees(radius).toFloat()
    }

    /**
     * 计算旋转的角度
     *
     * @param baseX 基准点x坐标
     * @param baseY 基准点y坐标
     * @param rotateX 旋转点x坐标
     * @param rotateY 旋转点y坐标
     * @return 旋转的角度
     */

    //计算并返回两个点之间旋转的角度(基于图片的中心点，即图片中心点为原点)
    internal fun callRotation(baseX: Float, baseY: Float, rotateX: Float, rotateY: Float): Float {
        val deltaX = (baseX - rotateX).toDouble()
        val deltaY = (baseY - rotateY).toDouble()
        //atan2 函数接受两个参数：deltaY（y 坐标的差值）和 deltaX（x 坐标的差值）。
        //返回值：atan2 返回的结果是一个 `弧度值` ，表示从 x 轴正方向顺时针旋转到指向点 (deltaX, deltaY) 的角度。
        //范围：返回的角度范围是 -π 到 π（-180° 到 180°）。
        val radius = atan2(deltaY, deltaX)
        // toDegrees()用于将 `弧度值` 转换为 `角度`
        return Math.toDegrees(radius).toFloat()
    }

    internal fun getMidPoint(event:MotionEvent):PointF{
        //第一个点的坐标
        val x1:Float = event.getX(0)
        val y1:Float = event.getY(0)
        //第二个点的坐标
        val x2:Float = event.getX(1)
        val y2:Float = event.getY(1)

        val midX = (x1+x2)/2f
        val midY = (y1+y2)/2f

        return PointF(midX,midY)

    }

    /**
     * 点击模式
     */
    internal enum class TouchMode {
        /**
         * 区域外
         */
        TOUCH_OUTSIDE,

        /**
         * 图片显示区域
         */
        TOUCH_IMAGE,

        /**
         * 旋转控制点
         */
        TOUCH_ROTATE,

        /**
         * 左上角控制点，等比缩放
         */
        TOUCH_CONTROL_1,

        /**
         * 右上角控制点，等比缩放
         */
        TOUCH_CONTROL_2,

        /**
         * 左下角控制点，等比缩放
         */
        TOUCH_CONTROL_3,

        /**
         * 右下角控制点，等比缩放
         */
        TOUCH_CONTROL_4,

        /**
         * 左中间控制点，横向缩放
         */
        TOUCH_CONTROL_5,

        /**
         * 右中间控制点，横向缩放
         */
        TOUCH_CONTROL_6,

        /**
         * 下中间控制点，竖向缩放
         */
        TOUCH_CONTROL_7,
    }
}