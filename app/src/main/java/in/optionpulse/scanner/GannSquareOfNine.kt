package com.optionpulse.scanner

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Square-of-9 price projection using the requested θ/180 convention.
 * All calculations use Double precision; rounding is a presentation concern.
 */
object GannSquareOfNine {
    val standardAngles = listOf(45, 90, 135, 180, 225, 270, 315, 360, 540, 720)

    fun factor(angleDegrees: Int): Double {
        require(angleDegrees >= 0) { "Angle must be non-negative" }
        return angleDegrees / 180.0
    }

    fun resistance(basePrice: Double, angleDegrees: Int): Double {
        require(basePrice > 0.0) { "Base price must be positive" }
        val shiftedRoot = sqrt(basePrice) + factor(angleDegrees)
        return shiftedRoot * shiftedRoot
    }

    fun support(basePrice: Double, angleDegrees: Int): Double {
        require(basePrice > 0.0) { "Base price must be positive" }
        val shiftedRoot = max(0.0, sqrt(basePrice) - factor(angleDegrees))
        return shiftedRoot * shiftedRoot
    }

    fun reverseAngle(currentPrice: Double, pivotPrice: Double): Double {
        require(currentPrice > 0.0 && pivotPrice > 0.0) { "Prices must be positive" }
        return abs(sqrt(currentPrice) - sqrt(pivotPrice)) * 180.0
    }

    fun levels(pivotPrice: Double): GannLevels = GannLevels(
        pivot = pivotPrice,
        resistance45 = resistance(pivotPrice, 45),
        resistance90 = resistance(pivotPrice, 90),
        resistance135 = resistance(pivotPrice, 135),
        resistance180 = resistance(pivotPrice, 180),
        resistance360 = resistance(pivotPrice, 360),
        support45 = support(pivotPrice, 45),
        support90 = support(pivotPrice, 90),
        support180 = support(pivotPrice, 180),
        support360 = support(pivotPrice, 360)
    )
}

data class GannLevels(
    val pivot: Double,
    val resistance45: Double,
    val resistance90: Double,
    val resistance135: Double,
    val resistance180: Double,
    val resistance360: Double,
    val support45: Double,
    val support90: Double,
    val support180: Double,
    val support360: Double
)

data class GannTradePlan(
    val trigger: Double,
    val protectiveLevel: Double,
    val target1: Double,
    val target2: Double
)

fun GannLevels.callPlan(): GannTradePlan = GannTradePlan(
    trigger = resistance90,
    protectiveLevel = resistance45,
    target1 = resistance180,
    target2 = resistance360
)

fun GannLevels.putPlan(): GannTradePlan = GannTradePlan(
    trigger = support90,
    protectiveLevel = support45,
    target1 = support180,
    target2 = support360
)
