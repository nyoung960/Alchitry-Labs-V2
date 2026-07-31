package com.alchitry.labs2.ui.graphing

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.ui.theme.AlchitryColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * A single point on a [RealtimeGraph].
 *
 * Points added to a [RealtimeGraphState] must have monotonically increasing [x] values, but
 * they do not need to be evenly spaced.
 */
data class GraphPoint(
    val x: Float,
    val y: Float
)

/**
 * Holds the points displayed by a [RealtimeGraph].
 *
 * Points can be manipulated at any time and the graph will automatically update. The x values
 * of the points are assumed to be monotonically increasing but may be unevenly spaced.
 */
class RealtimeGraphState(initialPoints: List<GraphPoint> = emptyList()) {
    private val pointList = mutableStateListOf<GraphPoint>().apply { addAll(initialPoints) }

    /** Read-only view of the current points. */
    val points: List<GraphPoint> get() = pointList

    val size: Int get() = pointList.size

    operator fun get(index: Int): GraphPoint = pointList[index]

    operator fun set(index: Int, point: GraphPoint) {
        pointList[index] = point
    }

    fun add(point: GraphPoint) {
        pointList.add(point)
    }

    /** Adds a point with the given y [value] at the given [x] position. */
    fun add(value: Number, x: Number) {
        pointList.add(GraphPoint(x.toFloat(), value.toFloat()))
    }

    fun addAll(points: Collection<GraphPoint>) {
        pointList.addAll(points)
    }

    fun removeAt(index: Int): GraphPoint = pointList.removeAt(index)

    /** Removes the first [count] points. Useful for trimming old data. */
    fun removeFirst(count: Int) {
        pointList.removeRange(0, count.coerceAtMost(pointList.size))
    }

    fun clear() {
        pointList.clear()
    }

    /**
     * Returns the index of the first point whose x value is >= [x], or [size] if all points
     * are smaller. Assumes the points are sorted by x.
     */
    fun indexAtOrAfter(x: Float): Int {
        var low = 0
        var high = pointList.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (pointList[mid].x < x) low = mid + 1 else high = mid
        }
        return low
    }

    /** Returns the index of the point whose x value is closest to [x], or null if empty. */
    fun nearestIndex(x: Float): Int? {
        if (pointList.isEmpty()) return null
        val after = indexAtOrAfter(x)
        if (after == 0) return 0
        if (after >= pointList.size) return pointList.size - 1
        val before = after - 1
        return if (x - pointList[before].x <= pointList[after].x - x) before else after
    }
}

/**
 * Shared horizontal pan/zoom and cursor state for one or more [RealtimeGraph] composables.
 *
 * Pass the same [GraphLinkState] to multiple graphs to keep their horizontal scrolling in sync
 * and to show the cursor line/values on all of them when hovering over any one of them.
 */
class GraphLinkState(
    initialXScale: Float = 100f
) {
    /** X value (in graph units) shown at the left edge of the graph. */
    var xOffset by mutableStateOf(0f)

    /** Number of pixels per graph x unit (zoom level). */
    var xScale by mutableStateOf(initialXScale)

    /** Cursor x position in pixels relative to the graph, or null when not hovering. */
    var cursorX by mutableStateOf<Float?>(null)

    /** True while a drag (pan) gesture is active on any linked graph. */
    var dragging by mutableStateOf(false)

    var minXScale = 0.001f
    var maxXScale = 100_000f

    /** Width of the graph viewport in pixels, updated by [RealtimeGraph]. */
    var viewportWidth by mutableStateOf(0f)

    /** Data x-ranges reported by each linked graph, keyed by the graph's state. */
    private val dataRanges = mutableStateMapOf<Any, ClosedFloatingPointRange<Float>>()

    private val minDataX: Float get() = dataRanges.values.minOfOrNull { it.start } ?: 0f
    private val maxDataX: Float get() = dataRanges.values.maxOfOrNull { it.endInclusive } ?: 0f

    /**
     * True when the view is pinned to the newest data. While pinned, the graph auto-scrolls
     * left as new points are added so the latest data stays visible. Scrolling away from the
     * end unpins the view; scrolling back to the end pins it again.
     */
    var followingEnd by mutableStateOf(true)

    private val minOffset: Float get() = minDataX

    private val maxOffset: Float
        get() = (maxDataX - viewportWidth / xScale).coerceAtLeast(minDataX)

    private fun clampOffset(offset: Float): Float =
        offset.coerceIn(minOffset, maxOffset)

    private fun updateFollowing() {
        followingEnd = xOffset >= maxOffset
    }

    /**
     * Called by [RealtimeGraph] to report the data range of one linked graph (identified by
     * [source]) and the viewport width. The scroll bounds are computed from the union of all
     * linked graphs' ranges so linked graphs stay aligned even when the view is outside a
     * single graph's data. Keeps the offset within the combined bounds and auto-scrolls to
     * show new data when [followingEnd] is set.
     */
    fun updateDataRange(source: Any, minX: Float, maxX: Float, viewportWidthPx: Float) {
        dataRanges[source] = minX..maxX
        viewportWidth = viewportWidthPx
        xOffset = if (followingEnd && !dragging) maxOffset else clampOffset(xOffset)
    }

    /** Removes the data range reported by [source], e.g. when a graph is removed or cleared. */
    fun removeDataRange(source: Any) {
        dataRanges.remove(source)
        xOffset = clampOffset(xOffset)
    }

    /** The graph x value under the cursor, or null when not hovering. */
    val cursorGraphX: Float?
        get() = cursorX?.let { it / xScale + xOffset }

    /** Converts a graph x value to a screen x position in pixels. */
    fun toScreenX(graphX: Float): Float = (graphX - xOffset) * xScale

    /** Converts a screen x position in pixels to a graph x value. */
    fun toGraphX(screenX: Float): Float = screenX / xScale + xOffset

    /** Pans the view by [amount] pixels. */
    fun scrollBy(amount: Float) {
        xOffset = clampOffset(xOffset + amount / xScale)
        updateFollowing()
    }

    /** Scrolls so the left edge of the view is at [px] pixels from the start of the data. */
    fun scrollTo(px: Float) {
        xOffset = clampOffset(minDataX + px / xScale)
        updateFollowing()
    }

    /** The current scroll position in pixels from the start of the data. */
    val scrollPosition: Float get() = (xOffset - minDataX) * xScale

    /**
     * Zooms by [factor] (values > 1 zoom in) keeping the point at screen position [centerX]
     * stationary.
     */
    fun zoomBy(factor: Float, centerX: Float) {
        val newScale = (xScale * factor).coerceIn(minXScale, maxXScale)
        val graphX = toGraphX(centerX)
        xScale = newScale
        xOffset = clampOffset(graphX - centerX / newScale)
        updateFollowing()
    }
}

@Composable
fun rememberGraphLinkState(initialXScale: Float = 100f): GraphLinkState =
    remember { GraphLinkState(initialXScale) }

/**
 * Applies pan (drag), zoom (scroll wheel), and cursor tracking gestures for [link].
 *
 * This is applied automatically by [RealtimeGraph]. It is exposed so a parent container holding
 * several linked graphs can also handle gestures in the space between them.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.graphGestures(link: GraphLinkState): Modifier = composed {
    val scope = rememberCoroutineScope()
    val tracker = remember { VelocityTracker() }
    val decay = rememberSplineBasedDecay<Float>()
    var flingJob by remember { mutableStateOf<Job?>(null) }

    fun stopFling() {
        tracker.resetTracking()
        flingJob?.cancel()
        flingJob = null
    }

    this
        .onPointerEvent(PointerEventType.Scroll) { event ->
            val scroll = event.changes.fold(0f) { acc, c -> acc + c.scrollDelta.y }
            if (scroll != 0f) {
                stopFling()
                val centerX = event.changes.first().position.x
                link.zoomBy(1f - scroll / 20f, centerX)
                link.cursorX = centerX
            }
        }
        .onPointerEvent(PointerEventType.Move) { event ->
            link.cursorX = event.changes.first().position.x
        }
        .onPointerEvent(PointerEventType.Exit) {
            if (!link.dragging)
                link.cursorX = null
        }
        .onPointerEvent(PointerEventType.Press) { stopFling() }
        .pointerInput(link) {
            detectDragGestures(
                onDragStart = {
                    stopFling()
                    link.dragging = true
                },
                onDragEnd = {
                    val velocity = tracker.calculateVelocity().x
                    tracker.resetTracking()
                    flingJob = scope.launch {
                        AnimationState(
                            initialValue = link.scrollPosition,
                            initialVelocity = -velocity
                        ).animateDecay(decay) {
                            link.scrollTo(value)
                        }
                        link.dragging = false
                    }
                },
                onDragCancel = {
                    tracker.resetTracking()
                    link.dragging = false
                }
            ) { change, dragAmount ->
                tracker.addPointerInputChange(change)
                change.consume()
                link.scrollBy(-dragAmount.x)
            }
        }
}

/**
 * A realtime graph that renders only the currently visible points.
 *
 * Points are placed by their x value, which must be monotonically increasing but may be
 * unevenly spaced. Supports zooming with the scroll wheel, panning by dragging, and shows the
 * point nearest the cursor along with a vertical cursor line. Multiple graphs sharing the same
 * [link] scroll, zoom, and show their cursors together.
 *
 * @param state the points to display
 * @param modifier layout modifier, should define the graph's size
 * @param link shared pan/zoom/cursor state, pass the same instance to multiple graphs to link them
 * @param color the color of the plotted line
 * @param minValue fixed lower bound for the y-axis, or null to auto-scale to the visible points
 * @param maxValue fixed upper bound for the y-axis, or null to auto-scale to the visible points
 * @param valueFormatter converts the point under the cursor into the text shown at the cursor
 */
@Composable
fun RealtimeGraph(
    state: RealtimeGraphState,
    modifier: Modifier = Modifier.fillMaxWidth().height(150.dp),
    link: GraphLinkState = rememberGraphLinkState(),
    color: Color = AlchitryColors.current.Accent,
    minValue: Float? = null,
    maxValue: Float? = null,
    valueFormatter: (GraphPoint) -> String = { it.y.toString() }
) {
    val graphPadding = 15.dp
    val cursorLineColor = MaterialTheme.colorScheme.onSurface

    // Stop contributing to the shared scroll bounds when this graph is removed.
    DisposableEffect(link, state) {
        onDispose { link.removeDataRange(state) }
    }

    Box(
        modifier
            .background(MaterialTheme.colorScheme.background)
            .graphGestures(link)
            .clipToBounds()
    ) {
        var valueScreenY by remember { mutableStateOf(Float.NaN) }
        var cursorPoint by remember { mutableStateOf<GraphPoint?>(null) }

        Canvas(Modifier.matchParentSize().padding(vertical = graphPadding)) {
            val points = state.points

            // Report the data bounds so scrolling is clamped to the combined data range of
            // all linked graphs and the view auto-scrolls to show new points when pinned to
            // the end.
            if (points.isNotEmpty()) {
                link.updateDataRange(state, points.first().x, points.last().x, size.width)
            } else {
                link.removeDataRange(state)
            }

            val offset = link.xOffset
            val scale = link.xScale

            // Only consider the points that are currently visible (plus one on each side so
            // lines entering/leaving the viewport are still drawn).
            val firstIndex = (state.indexAtOrAfter(offset) - 1).coerceAtLeast(0)
            val lastIndex = state.indexAtOrAfter(offset + size.width / scale)
                .coerceAtMost(points.size - 1)

            valueScreenY = Float.NaN
            cursorPoint = null

            if (lastIndex < firstIndex) return@Canvas

            var min = minValue ?: Float.POSITIVE_INFINITY
            var max = maxValue ?: Float.NEGATIVE_INFINITY
            if (minValue == null || maxValue == null) {
                for (i in firstIndex..lastIndex) {
                    val y = points[i].y
                    if (minValue == null && y < min) min = y
                    if (maxValue == null && y > max) max = y
                }
            }
            val range = (max - min).let { if (it <= 0f) 1f else it }

            fun screenY(value: Float) = size.height - (value - min) / range * size.height

            val path = Path()
            for (i in firstIndex..lastIndex) {
                val point = points[i]
                val x = link.toScreenX(point.x)
                val y = screenY(point.y)
                if (i == firstIndex) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(2f))

            link.cursorGraphX?.let { graphX ->
                state.nearestIndex(graphX)?.let { index ->
                    val point = points[index]
                    cursorPoint = point
                    valueScreenY = screenY(point.y) + graphPadding.toPx()
                }
            }

            // Highlight the exact cursor position with a vertical line.
            link.cursorX?.let { cursorX ->
                drawLine(
                    cursorLineColor,
                    Offset(cursorX, -graphPadding.toPx()),
                    Offset(cursorX, size.height + graphPadding.toPx())
                )
            }
        }

        val cursorX = link.cursorX
        val shownPoint = cursorPoint
        if (cursorX != null && shownPoint != null) {
            Layout(
                modifier = Modifier.matchParentSize(),
                content = {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.onSurface, CircleShape)
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                        tonalElevation = 5.dp,
                        shadowElevation = 5.dp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(Modifier.padding(2.5.dp)) {
                            Text(valueFormatter(shownPoint))
                        }
                    }
                }
            ) { measurables, constraints ->
                val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }

                // The parent may measure this layout with infinite constraints (e.g. during
                // intrinsic measurements), which aren't valid layout dimensions.
                val width = if (constraints.hasBoundedWidth) constraints.maxWidth else 0
                val height = if (constraints.hasBoundedHeight) constraints.maxHeight else 0

                layout(width, height) {
                    val dotX = link.toScreenX(shownPoint.x)
                    if (valueScreenY.isFinite()) {
                        placeables.getOrNull(0)?.let { dot ->
                            dot.place(
                                dotX.roundToInt() - dot.width / 2,
                                valueScreenY.roundToInt() - dot.height / 2
                            )
                        }
                    }

                    val labelPosition = if (valueScreenY.isFinite()) {
                        valueScreenY.roundToInt()
                    } else {
                        height / 2
                    }

                    placeables.getOrNull(1)?.let { tooltip ->
                        val x = (cursorX.roundToInt() + 10.dp.roundToPx())
                            .coerceAtMost(width - tooltip.width)
                        tooltip.place(
                            x,
                            (labelPosition - tooltip.height / 2)
                                .coerceIn(0, (height - tooltip.height).coerceAtLeast(0))
                        )
                    }
                }
            }
        }
    }
}
