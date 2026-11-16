package com.elal.flightsdk.presentation

import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Places the status badge inside the system's large-icon frame (spike). The system center-crops the large icon into
 * a ~16:9 rounded frame, so a wider image loses its ends; drawing the badge whole, centred on a transparent canvas of
 * the frame's shape, keeps all of it visible.
 */
internal object BadgeFit {

    /** Width : height of the large-icon frame seen on the Pixel and the S25+ (2026-11-16). */
    const val FRAME_ASPECT_WIDTH = 16
    const val FRAME_ASPECT_HEIGHT = 9

    /**
     * The badge's bounds inside a [frameWidth] × [frameHeight] canvas: as large as fits with [padding] (a fraction of
     * each side) kept free, aspect ratio of [badgeWidth] × [badgeHeight] preserved, centred.
     */
    fun centered(frameWidth: Int, frameHeight: Int, badgeWidth: Int, badgeHeight: Int, padding: Float): FitRect {
        val availableWidth = frameWidth * (1 - 2 * padding)
        val availableHeight = frameHeight * (1 - 2 * padding)
        val scale = min(availableWidth / badgeWidth, availableHeight / badgeHeight)
        val width = (badgeWidth * scale).roundToInt()
        val height = (badgeHeight * scale).roundToInt()
        val left = (frameWidth - width) / 2
        val top = (frameHeight - height) / 2
        return FitRect(left, top, left + width, top + height)
    }
}
