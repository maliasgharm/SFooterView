package org.noandish.library.footerview

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.get
import androidx.core.view.marginBottom
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.shape.CornerFamily
import org.noandish.library.footerview.FooterView.Status.*
import org.noandish.library.footerview.Utils.requestSize


private const val MIN_DURATION_ANIMATION = 20L
private const val MAX_DURATION_ANIMATION = 800L

@SuppressLint("ClickableViewAccessibility", "Recycle")
class FooterView @JvmOverloads constructor(
    context: Context, val attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : NestedScrollView(context, attrs, defStyleAttr) {
    private val spaceView = View(context)

    private var layoutHandler = LinearLayout(context)

    private var layoutPinned = LinearLayout(context)

    private var layoutContent = LinearLayout(context)

    private val mainLayer = LinearLayout(context)

    private val cardView = MaterialCardView(context)

    var closable = true

    var shadowBehind = false
        set(value) {
            field = value
        }

    var backgroundColorCard = Color.WHITE
        set(value) {
            field = value
            cardView.setCardBackgroundColor(backgroundColorCard)
        }
    var percentOpen: Float = 0f
        private set(value) {
            field = value
            if (background is ColorDrawable) {
                if (shadowBehind) {
                    val color = Color.argb(
                        (percentOpen * 100).toInt(),
                        (background as ColorDrawable).color.red,
                        (background as ColorDrawable).color.green,
                        (background as ColorDrawable).color.blue
                    )
                    setBackgroundColor(color)
                } else {
                    setBackgroundColor(Color.TRANSPARENT)
                }
            } else {
                setBackgroundColor(Color.TRANSPARENT)
            }
        }

    var percentFullOpen: Float = 0f
        private set

    var cornerTopRight: Float? = null
        set(value) {
            if (value != null && field != null)
                changeCorner(topRight = value)
            field = value
        }
    var cornerTopLeft: Float? = null
        set(value) {
            if (value != null && field != null)
                changeCorner(topLeft = value)
            field = value
        }
    var cornerBottomRight: Float? = null
        set(value) {
            if (value != null && field != null)
                changeCorner(bottomRight = value)
            field = value
        }
    var cornerBottomLeft: Float? = null
        set(value) {
            if (value != null && field != null)
                changeCorner(bottomLeft = value)
            field = value
        }


    val handlerView: View?
        get() = if (layoutHandler.childCount > 0)
            layoutHandler.getChildAt(0)
        else
            null

    var status: Status? = null
        private set(value) {
            field = value
            if (field == null)
                return
            listenersStatus.forEach { it.invoke(value!!) }
        }

    var startStatus = CLOSE
        set(value) {
            field = value
            when (value) {
                CLOSE -> scrollTo(0, 0)
                OPEN -> scrollTo(0, openValue.toInt())
                FULL_OPEN -> scrollTo(0, fullOpenValue.toInt())
            }
        }

    var direction: Direction? = null
        private set


    private val listenersStatus = ArrayList<(status: Status) -> Unit>()

    private val listenersScroll = ArrayList<(direction: Direction?, y: Int) -> Unit>()

    var touchableHandler = false

    private var lastEventTouch: MotionEvent? = null

     val openValue: Float
        get() {
            return layoutContent.top.toFloat()
        }
    private val fullOpenValue: Float
        get() {
            return cardView.measuredHeight.toFloat() + openValue
        }
    var cardElevation = 1f
        set(value) {
            field = value
//            cardView.cardElevation = field
            cardView.cardElevation = 10f
            cardView.maxCardElevation = 10f

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                cardView.elevation = 10f
            }

        }
    private val linerLayoutScrollContent = LinearLayout(context)

    init {


        val ta = context.obtainStyledAttributes(attrs, R.styleable.FooterView)
        if (attrs != null) {
            cornerTopRight = ta.getDimension(R.styleable.FooterView_cornerTopRight, 15f)
            cornerTopLeft = ta.getDimension(R.styleable.FooterView_cornerTopLeft, 15f)
            cornerBottomRight = ta.getDimension(R.styleable.FooterView_cornerBottomRight, 0f)
            cornerBottomLeft = ta.getDimension(R.styleable.FooterView_cornerBottomLeft, 0f)
            backgroundColorCard =
                ta.getColor(R.styleable.FooterView_backgroundColorCard, backgroundColorCard)
            changeCorner(cornerTopRight!!, cornerTopLeft!!, cornerBottomRight!!, cornerBottomLeft!!)

            startStatus = values()[ta.getInt(R.styleable.FooterView_startType, 0)]
            shadowBehind = ta.getBoolean(R.styleable.FooterView_shadowBehind, shadowBehind)
            closable = ta.getBoolean(R.styleable.FooterView_closable, closable)
            touchableHandler =
                ta.getBoolean(R.styleable.FooterView_touchableHandler, touchableHandler)
            cardElevation = ta.getDimension(R.styleable.FooterView_cardElevation, cardElevation)

        }
        overScrollMode = View.OVER_SCROLL_NEVER
        mainLayer.orientation = LinearLayout.VERTICAL
        layoutHandler.orientation = LinearLayout.VERTICAL
        layoutPinned.orientation = LinearLayout.VERTICAL
        layoutContent.orientation = LinearLayout.VERTICAL

        layoutHandler.gravity = Gravity.CENTER
        cardView.setCardBackgroundColor(backgroundColorCard)
        linerLayoutScrollContent.orientation = LinearLayout.VERTICAL
        linerLayoutScrollContent.addView(
            spaceView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Utils.heightScreen
            )
        )
        val paramsCardView =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

        linerLayoutScrollContent.addView(
            cardView, paramsCardView
        )
        super.addView(
            linerLayoutScrollContent,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        //  linerLayoutScrollContent.setPadding(0,0,0,100)

        cardView.addView(
            mainLayer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        mainLayer.addView(
            layoutHandler,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        mainLayer.addView(
            layoutPinned,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        mainLayer.addView(
            layoutContent,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        listenersScroll.add { direction, y ->
            percentFullOpen = if (y > fullOpenValue) 1f else (y / fullOpenValue)
            percentOpen = if (y > openValue) 1f else (y / openValue)
        }
        requestSize {
            val params = spaceView.layoutParams
            params.height = measuredHeight + 5
            spaceView.layoutParams = params
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        requestSize {
            startStatus = startStatus
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    var valueAnimator: ValueAnimator? = null
    fun open(durationAnimation: Long = duration) {
        animateToY(openValue, durationAnimation) {
            changeStatusTo(OPEN)
        }
    }

    fun fullOpen(durationAnimation: Long = duration) {
        animateToY(fullOpenValue, durationAnimation) {
            changeStatusTo(FULL_OPEN)
        }
    }

    private fun changeStatusTo(status: Status) {
        if (this.status != status)
            this.status = status
    }

    fun close(durationAnimation: Long = duration) {
        if (closable)
            animateToY((0).toFloat(), durationAnimation) {
                changeStatusTo(CLOSE)
            }
    }

    private fun animateToY(
        toY: Float,
        durationAnimation: Long = duration,
        listenerAnimation: () -> Unit
    ) {
        valueAnimator?.cancel()
        cardView.apply {
            valueAnimator = ObjectAnimator.ofFloat(
                this@FooterView.scrollY.toFloat(), toY
            )

            valueAnimator?.apply {
                addUpdateListener {
                    this@FooterView.scrollTo(0, (it.animatedValue as Float).toInt())
                    if (it.animatedValue == toY) {
                        listenerAnimation.invoke()
                    }
                }
                duration = durationAnimation
                start()
            }
        }

    }

    fun changeCorner(
        topRight: Float = -1f,
        topLeft: Float = -1f,
        bottomRight: Float = -1f,
        bottomLeft: Float = -1f
    ) {
        val shapeAppearanceModelBuilder = cardView.shapeAppearanceModel
            .toBuilder()
        if (topRight >= 0)
            shapeAppearanceModelBuilder.setTopRightCorner(CornerFamily.ROUNDED, topRight)
        if (topLeft >= 0)
            shapeAppearanceModelBuilder.setTopLeftCorner(CornerFamily.ROUNDED, topLeft)
        if (bottomRight >= 0)
            shapeAppearanceModelBuilder.setBottomRightCorner(CornerFamily.ROUNDED, bottomRight)
        if (bottomLeft >= 0)
            shapeAppearanceModelBuilder.setBottomLeftCorner(CornerFamily.ROUNDED, bottomLeft)
        cardView.shapeAppearanceModel = shapeAppearanceModelBuilder.build()
    }

    fun setPinnedView(view: View) {
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ) as LayoutParams
        params.isPinedView = true
        addView(view, params)
    }

    override fun addView(child: View?, params: ViewGroup.LayoutParams?) {
        if (params is LayoutParams) {
            if (params.isHandlerView) {
                layoutHandler.addView(child, params)
            } else if (params.isPinedView) {
                layoutPinned.addView(child, params)
            } else {
                layoutContent.addView(child, params)
            }
        }
    }


    private var lYDown = 0f
    private var lScrollY = 0
    private var duration = 0L
        set(value) {
            field = when {
                value < MIN_DURATION_ANIMATION -> MIN_DURATION_ANIMATION
                value > MAX_DURATION_ANIMATION -> MAX_DURATION_ANIMATION
                else -> value * 10
            }
        }
    private var lastTimeEvent = 0L
    private val listDurationAnimationAve = ArrayList<Long>()

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (touchableHandler)
            return false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                valueAnimator?.cancel()
                valueAnimator?.clone()
                listDurationAnimationAve.clear()
                lYDown = ev.y
                lScrollY = scrollY
            }
            MotionEvent.ACTION_MOVE -> {
                if (listDurationAnimationAve.size > 5) {
                    listDurationAnimationAve.removeAt(0)
                }
                listDurationAnimationAve.add(System.currentTimeMillis())
                if (!closable && scrollY < openValue) {
                    return false
                }
            }
            MotionEvent.ACTION_UP -> {
                duration = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    System.currentTimeMillis() - calculateAverage(listDurationAnimationAve).toFloat(),
                    resources.displayMetrics
                )
                    .toLong()
//                            .toLong()
                scrollToCorrectPosition()
                return false
            }
        }
        lastEventTouch = ev
        return if (lYDown + lScrollY > cardView.top) {
            return super.onTouchEvent(ev)
        } else false
    }

    private fun calculateAverage(marks: List<Long>): Long {
        var sum = 0L
        if (marks.isNotEmpty()) {
            for (mark in marks) {
                sum += mark
            }
            return (sum.toDouble() / marks.size).toLong()
        }
        return sum
    }
    val scrollYFixed = this.scrollY -5
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        direction = if (t > oldt) {
            Direction.OPENING
        } else {
            Direction.CLOSING
        }
        listenersScroll.forEach { it.invoke(direction!!, t-5) }
    }

    private fun scrollToCorrectPosition() {
        when (direction) {
            Direction.OPENING -> {
                if (scrollY > openValue)
                    fullOpen()
                else
                    open()
            }
            Direction.CLOSING -> {
                if (scrollY > openValue || !closable)
                    open()
                else {
                    close()
                }
            }
        }
    }


    private fun scrollToLastPosition() {
        when (status) {
            CLOSE -> scrollTo(0, 0)
            OPEN -> scrollTo(0, openValue.toInt())
            FULL_OPEN -> scrollTo(0, fullOpenValue.toInt())
        }
    }

//==================================================================================================

    override fun generateLayoutParams(attrs: AttributeSet?): LayoutParams? {
        return LayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): FrameLayout.LayoutParams? {
        return LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): FrameLayout.LayoutParams? {
        return FrameLayout.LayoutParams(p)
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LayoutParams
    }

    @SuppressLint("Recycle", "CustomViewStyleable")
    class LayoutParams(c: Context?, attrs: AttributeSet?) :
        FrameLayout.LayoutParams(c, attrs) {
        var isPinedView = false
        var isHandlerView = false

        init {
            val ta = c?.obtainStyledAttributes(attrs, R.styleable.FooterViewLP)
            isPinedView =
                ta?.getBoolean(R.styleable.FooterViewLP_layout_isPined, isPinedView)
                    ?: isPinedView
            isHandlerView =
                ta?.getBoolean(R.styleable.FooterViewLP_layout_isHandler, isHandlerView)
                    ?: isHandlerView
        }
    }

    fun addOnStatusChangeListener(listener: (status: Status) -> Unit) {
        this.listenersStatus.add(listener)
    }

    fun addOnChangeScrollListener(listener: (status: Direction?, scrollY: Int) -> Unit) {
        this.listenersScroll.add(listener)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        if (linerLayoutScrollContent[0].layoutParams.height <= 0) {
            val params = linerLayoutScrollContent[0].layoutParams
            params.height = h + marginBottom
            linerLayoutScrollContent[0].layoutParams = params
            super.onSizeChanged(w, h, oldw, oldh)
        }
        scrollToLastPosition()
    }

    enum class Status {
        CLOSE,
        OPEN,
        FULL_OPEN,
    }

    enum class Direction {
        CLOSING,
        OPENING
    }

    companion object {
        private const val TAG = "FooterView"

    }

}