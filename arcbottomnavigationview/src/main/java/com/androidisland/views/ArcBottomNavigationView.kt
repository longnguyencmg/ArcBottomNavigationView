package com.androidisland.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.iterator
import androidx.core.view.size
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlin.random.Random

@SuppressLint("RestrictedApi")
class ArcBottomNavigationView : BottomNavigationView {

    companion object {
        private const val TAG = "ArcBottomNavigationView"
        //Dimens are in Dp
        private const val DEFAULT_BUTTON_SIZE = 56
        private const val DEFAULT_BUTTON_MARGIN = 8
        private const val DEFAULT_BUTTON_STROKE_WIDTH = 0
        private const val DEFAULT_BUTTON_STROKE_COLOR = Color.TRANSPARENT

        private const val DEFAULT_ANIM_DURATION = 200L
        private const val CURVE_MAX_POINTS = 100
    }

    private var button: MaterialButton? = null
    private var menuView: BottomNavigationMenuView

    //TODO mamrgin property, invlaidate draw...
    private var buttonMargin = DEFAULT_BUTTON_MARGIN.toPixel()
    private var buttonRadius = (DEFAULT_BUTTON_SIZE / 2).toPixel()
    var buttonIcon: Drawable? = null
        set(value) {
            field = value
            button?.apply {
                icon = value
            }
        }
    var buttonIconSize: Float = buttonRadius
        set(value) {
            field = value
            button?.apply {
                iconSize = value.toInt()
            }
        }

    var buttonStrokeWidth: Float = 0.0f
        set(value) {
            field = value
            button?.apply {
                strokeWidth = value.toInt()
            }
        }

    var buttonStrokeColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            button?.apply {
                strokeColor = ColorStateList.valueOf(value)
            }
        }

    var buttonIconTint: Int = Color.TRANSPARENT
        set(value) {
            field = value
            button?.apply {
                if (value != Color.TRANSPARENT)
                    iconTint = ColorStateList.valueOf(value)
            }
        }

    var buttonBackgroundTint: Int = Color.TRANSPARENT
        set(value) {
            field = value
            button?.apply {
                if (value != Color.TRANSPARENT)
                    supportBackgroundTintList = ColorStateList.valueOf(value)
            }
        }

    var buttonRippleColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            button?.apply {
                rippleColor = ColorStateList.valueOf(value)
            }
        }

    private var currentState: State = State.FLAT
    var state: State = currentState
        set(value) {
            field = value
            transitionTo(value)
        }

    private var animator: ValueAnimator? = null
    private lateinit var visibleBound: RectF
    //Keeps curved state points, only change when size changed
    private lateinit var curvedBoundPoints: MutableList<PointF>
    //Keeps flat state points, only change when size changed
    private lateinit var flatBoundPoints: MutableList<PointF>
    //Keeps points for current state, every time we assign it a list
    //it changes currentPath and redraws view
    //assignment should only happen in animation updates
    private var currentPoints: MutableList<PointF> = mutableListOf()
        set(value) {
            field = value
            currentPath = pointsToPath(field)
            invalidate()
        }
    private var currentPath = Path()
    private val invisibleMenuItemId = Random(System.currentTimeMillis()).nextInt()
    private val navMenu = menu as MenuBuilder
    private var itemSelectedListener: OnNavigationItemSelectedListener? = null
    var buttonClickListener: ((arcBottomNavView: ArcBottomNavigationView) -> Unit)? = null
    private var selectedItemIndex = 0

    private val curvePath = Path()
    private val rectPath = Path()
    //For debug purpose only
    private val circle = Path()

    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x55000000
        style = Paint.Style.FILL
    }

    val controlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        strokeWidth = 12.0f
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, 0) {
        var buttonSize = DEFAULT_BUTTON_SIZE.toPixel()
        buttonIconSize = buttonSize / 2
        buttonStrokeWidth = DEFAULT_BUTTON_STROKE_WIDTH.toPixel()
        buttonStrokeColor = DEFAULT_BUTTON_STROKE_COLOR

        val typedValue = TypedValue()
        val typedArray = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorAccent))
        buttonBackgroundTint = typedArray.getColor(0, Color.BLACK)
        typedArray.recycle()

        attrs?.apply {
            val ta = context.obtainStyledAttributes(this, R.styleable.ArcBottomNavigationView)
            buttonSize = ta.getDimension(R.styleable.ArcBottomNavigationView_ai_buttonSize, buttonSize)
            buttonMargin = ta.getDimension(R.styleable.ArcBottomNavigationView_ai_buttonMargin, buttonMargin)
            if (buttonMargin < DEFAULT_BUTTON_MARGIN.toPixel()) buttonMargin = DEFAULT_BUTTON_MARGIN.toPixel()
            buttonIcon = ta.getDrawable(R.styleable.ArcBottomNavigationView_ai_buttonIcon)
            buttonIconSize = ta.getDimension(R.styleable.ArcBottomNavigationView_ai_buttonIconSize, buttonSize / 2)
            buttonIconSize = Math.min(buttonSize / 2, buttonIconSize)
            buttonStrokeWidth =
                ta.getDimension(R.styleable.ArcBottomNavigationView_ai_buttonStrokeWidth, buttonStrokeWidth)
            buttonStrokeColor = ta.getColor(R.styleable.ArcBottomNavigationView_ai_buttonStrokeColor, buttonStrokeColor)
            buttonBackgroundTint =
                ta.getColor(R.styleable.ArcBottomNavigationView_ai_buttonBackgroundTint, buttonBackgroundTint)
            buttonRippleColor =
                ta.getColor(R.styleable.ArcBottomNavigationView_ai_buttonRippleColor, buttonRippleColor)
            buttonIconTint = ta.getColor(R.styleable.ArcBottomNavigationView_ai_buttonIconTint, buttonIconTint)
            val state = ta.getInt(R.styleable.ArcBottomNavigationView_ai_state, 1)
            currentState = if (state == 1) State.FLAT else State.ARC
            ta.recycle()
        }
        buttonRadius = buttonSize / 2

        ViewCompat.setElevation(this, 0.0f)
        menuView = getChildAt(0) as BottomNavigationMenuView
        menuView.layoutParams.apply {
            (this as LayoutParams).gravity = Gravity.BOTTOM
        }
        menuView.bringToFront()
        //Creates button
        val contextWrapper = ContextThemeWrapper(context, R.style.ArcTheme)
        button = MaterialButton(contextWrapper, null, R.attr.materialButtonStyle)
            .apply {
                layoutParams = LayoutParams(buttonSize.toInt(), buttonSize.toInt(), Gravity.TOP or Gravity.CENTER)
                cornerRadius = (buttonSize / 2).toInt()
                gravity = Gravity.CENTER
                iconPadding = 0
                iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
                iconSize = buttonIconSize.toInt()
                icon = buttonIcon
                iconTintMode = PorterDuff.Mode.SRC_IN
                if (buttonIconTint != Color.TRANSPARENT)
                    iconTint = ColorStateList.valueOf(buttonIconTint)
                strokeWidth = buttonStrokeWidth.toInt()
                strokeColor = ColorStateList.valueOf(buttonStrokeColor)
                if (buttonBackgroundTint != Color.TRANSPARENT)
                    supportBackgroundTintList = ColorStateList.valueOf(buttonBackgroundTint)
                rippleColor = ColorStateList.valueOf(buttonRippleColor)
                visibility = if (currentState == State.FLAT) View.INVISIBLE else View.VISIBLE
                setOnClickListener {
                    buttonClickListener?.invoke(this@ArcBottomNavigationView)
                }
                setTextColor(Color.TRANSPARENT)
                addView(this, 1)
            }
        ViewCompat.setElevation(button!!, 0.0f)
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        visibleBound = getVisibleBound()
        curvedBoundPoints = createCurveBoundPoints(w.toFloat())
        flatBoundPoints = createFlatBoundPoints(curvedBoundPoints)
        currentPoints = if (currentState == State.FLAT) flatBoundPoints else curvedBoundPoints
    }

    override fun isItemHorizontalTranslationEnabled(): Boolean {
        return false
    }

    override fun setItemHorizontalTranslationEnabled(itemHorizontalTranslationEnabled: Boolean) {

    }

    @SuppressLint("RestrictedApi")
    override fun onFinishInflate() {
        super.onFinishInflate()
        if (menu.size % 2 != 0) throw IllegalStateException("Item menu size should be even")
        regenerateMenu()
        navMenu.setCallback(object : MenuBuilder.Callback {
            override fun onMenuModeChange(menu: MenuBuilder?) {
            }

            override fun onMenuItemSelected(menu: MenuBuilder?, item: MenuItem?): Boolean {
                item?.apply {
                    selectedItemIndex = menu?.children?.indexOf(item) ?: -1
                    Log.d("test123", "selected=====> $selectedItemIndex")
                    if (itemId != invisibleMenuItemId) {
                        itemSelectedListener?.onNavigationItemSelected(this)
                    }
                }
                return false
            }
        })
    }

    private fun regenerateMenu() {
        val items = mutableListOf<MenuItem>().apply {
            menu.iterator().forEach {
                add(it)
            }
        }
        menu.clear()
        items.forEachIndexed { index, item ->
            val order = if (index < items.size / 2) 1 else items.size
            menu.add(item.groupId, item.itemId, order, item.title).apply {
                icon = item.icon
            }
        }
        val invisibleItem = menu.findItem(invisibleMenuItemId)
        if (invisibleItem == null) {
            menu.add(Menu.NONE, invisibleMenuItemId, menu.size / 2, "").apply {
                isEnabled = false
                isChecked = false
                isCheckable = false
            }
            if (selectedItemIndex >= menu.size / 2) selectedItemIndex++
        }
        updateInvisibleMenuItem(currentState)
    }

    private fun invisibleItemExists() = menu.size() % 2 == 1

    private fun invisibleMenuItem() = findViewById<View>(invisibleMenuItemId)

    private fun updateInvisibleMenuItem(state: State) {
        val item = menu.findItem(invisibleMenuItemId)
        item.isVisible = state != State.FLAT
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(width, measuredHeight + buttonRadius.toInt())
    }

    override fun dispatchDraw(canvas: Canvas?) {
        canvas?.restore()
        super.dispatchDraw(canvas)
    }

    override fun draw(canvas: Canvas?) {
        canvas?.save()
        canvas?.clipPath(currentPath)
        super.draw(canvas)
    }

    private fun isCorner(point: FloatArray): Boolean {
        getVisibleCorners().forEach {
            if (point[0] == it.x && point[1] == it.y) return true
        }
        return false
    }

    private fun isCorner(point: PointF) = isCorner(floatArrayOf(point.x, point.y))

    private fun getVisibleBound(): RectF {
        return RectF(0.0f, buttonRadius, width.toFloat(), height.toFloat())
    }


    private fun getVisibleCorners(): List<PointF> {
        return listOf(
            PointF(visibleBound.left, visibleBound.top),
            PointF(visibleBound.right, visibleBound.top),
            PointF(visibleBound.right, visibleBound.bottom),
            PointF(visibleBound.left, visibleBound.bottom)
        )
    }

    private fun curveStart(width: Float) =
        PointF(width / 2 - 1.2f * buttonRadius - 4.0f * buttonMargin, getVisibleCorners()[0].y)

    private fun curveEnd(width: Float) = PointF().apply {
        val curveStart = curveStart(width)
        x = width - curveStart.x
        y = curveStart.y
    }

    private fun createCurvePath(width: Float): Path {
        return Path().apply {
            val topLeft = getVisibleCorners()[0]
            val start = curveStart(width)
            val p2 = PointF(width / 2.toFloat() - 0.8f * buttonRadius, topLeft.y)
            val p3 =
                PointF(width / 2 - 1.0f * buttonRadius - buttonMargin * .8f, topLeft.y + buttonRadius + buttonMargin)
            val p4 = PointF(width / 2.toFloat(), topLeft.y + buttonRadius + buttonMargin)

            moveTo(start.x, start.y)
            cubicTo(p2.x, p2.y, p3.x, p3.y, p4.x, p4.y)

            val p5 = PointF(width - p3.x, p3.y)
            val p6 = PointF(width - p2.x, p2.y)
            val end = curveEnd(width)
            cubicTo(p5.x, p5.y, p6.x, p6.y, end.x, end.y)
        }
    }

    /**
     * Top left line of curve
     */
    private fun createTopLeftPath(width: Float): Path {
        return Path().apply {
            val topLeft = getVisibleCorners()[0]
            val curveStart = curveStart(width)
            moveTo(topLeft.x, topLeft.y)
            lineTo(curveStart.x, curveStart.y)
        }
    }

    /**
     * Top right line of curve
     */
    private fun createTopRightPath(width: Float): Path {
        return Path().apply {
            val topRight = getVisibleCorners()[1]
            val curveEnd = curveEnd(width)
            moveTo(curveEnd.x, curveEnd.y)
            lineTo(topRight.x, topRight.y)
        }
    }

    /**
     * Three lines in right bottom and left
     */
    private fun createRightBottomLeftPath(): Path {
        return Path().apply {
            val corners = getVisibleCorners()
            moveTo(corners[1].x, corners[1].y)
            lineTo(corners[2].x, corners[2].y)
            lineTo(corners[3].x, corners[3].y)
            lineTo(corners[0].x, corners[0].y)
        }
    }

    private fun createCurveBoundPoints(width: Float): MutableList<PointF> {
        return mutableListOf<PointF>().apply {

            val corners = getVisibleCorners()
            val curveStart = curveStart(width)
            val curveEnd = curveEnd(width)

            //top-right segment
            add(PointF(corners[0].x, corners[1].y))
            add(PointF(curveStart.x, curveStart.y))

            //Curve points
            val pm = PathMeasure()
            val point = FloatArray(2) { 0.0f }
            pm.setPath(createCurvePath(width), false)
            for (index in 0..CURVE_MAX_POINTS) {
                pm.getPosTan(pm.length * index / CURVE_MAX_POINTS.toFloat(), point, null)
                add(PointF(point[0], point[1]))
            }

            //top-left segment
            add(PointF(curveEnd.x, curveEnd.y))
            add(PointF(corners[1].x, corners[1].y))


            //right-bottom-left segment
            add(PointF(corners[2].x, corners[2].y))
            add(PointF(corners[3].x, corners[3].y))
            add(PointF(corners[0].x, corners[0].y))
        }
    }

    private fun createFlatBoundPoints(curveBoundPoints: MutableList<PointF>): MutableList<PointF> {
        return curveBoundPoints.clone().apply {
            forEach { point ->
                if (shouldAnimate(point)) {
                    point.y = visibleBound.top
                }
            }
        }
    }

    private fun pointsToPath(points: MutableList<PointF>): Path {
        return Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y)
                else lineTo(point.x, point.y)
            }
        }
    }

    /**
     * Check if the point is supposed to animate or not
     */
    private fun shouldAnimate(point: PointF) = point.y > visibleBound.top && point.y < visibleBound.bottom

    private fun transitionTo(state: State, duration: Long = DEFAULT_ANIM_DURATION) {
        if (state == currentState || animator?.isRunning == true) return
        animator?.apply {
            cancel()
        }

        animator = ValueAnimator.ofFloat(0.0f, 1.0f)
            .apply {
                this.duration = duration
                interpolator = FastOutSlowInInterpolator()
                val top = visibleBound.top
                //If dest state is FLAT init points are curved else flat
                val points =
                    if (state == State.FLAT) curvedBoundPoints.clone()
                    else flatBoundPoints.clone()
                val dest =
                    if (state == State.FLAT) flatBoundPoints else curvedBoundPoints

                val dy = FloatArray(points.size) { index ->
                    if (state == State.FLAT) {
                        points[index].y - dest[index].y
                    } else {
                        dest[index].y - points[index].y
                    }
                }
                addUpdateListener {
                    val animatedValue = it.animatedValue as Float
                    val factor = if (state == State.FLAT) 1.0f - animatedValue else animatedValue
                    points.forEachIndexed { index, point ->
                        if (!isCorner(point))
                            point.y = top + dy[index] * factor
                    }
                    currentPoints = points
                    animateButton(animatedValue, state)
                    onArcAnimationUpdate(animatedValue, currentState, state)
                }
                addListener(object : SimpleAnimatorListener() {
                    override fun onAnimationStart(animation: Animator?, isReverse: Boolean) {
                        super.onAnimationStart(animation, isReverse)
                        updateInvisibleMenuItem(state)
                        onArcAnimationStart(currentState, state)
                    }

                    override fun onAnimationEnd(animation: Animator?) {
                        super.onAnimationEnd(animation)
                        val from = currentState
                        currentState = state
                        onArcAnimationEnd(from, state)
                    }
                })
                start()
            }
    }

    private fun toggleState(state: State) = if (state == State.FLAT) State.ARC else State.FLAT


    public fun toggleTransition() {
        transitionTo(toggleState(currentState))
    }

    private fun MutableList<PointF>.clone(): MutableList<PointF> {
        return mutableListOf<PointF>().also { list ->
            forEach { point ->
                list.add(PointF(point.x, point.y))
            }
        }
    }

    override fun setOnNavigationItemSelectedListener(listener: OnNavigationItemSelectedListener?) {
        itemSelectedListener = listener
    }

    protected fun onArcAnimationStart(from: State, to: State) {
    }

    protected fun onArcAnimationUpdate(offset: Float, from: State, to: State) {
    }

    protected fun onArcAnimationEnd(from: State, to: State) {

    }

    private fun animateButton(offset: Float, to: State) {
        val scale = if (to == State.FLAT) 1.0f - offset else offset
        if (to == State.ARC && offset == 0.0f) button?.visibility = View.VISIBLE
        button?.scaleX = scale
        button?.scaleY = scale
        if (to == State.FLAT && offset == 1.0f) button?.visibility = View.INVISIBLE
    }

    private fun log(msg: String) = Log.d(TAG, msg)

    enum class State {
        ARC, FLAT
    }

    private open class SimpleAnimatorListener : Animator.AnimatorListener {

        override fun onAnimationRepeat(animation: Animator?) {

        }

        override fun onAnimationEnd(animation: Animator?) {
        }

        override fun onAnimationCancel(animation: Animator?) {
        }

        override fun onAnimationStart(animation: Animator?) {
        }
    }

    //TODO cancel anim on size change
    //TODO draw edit mode problem
    //TODO measure problem in frame layout
    //TODO clip replace with draw to use antialias
}