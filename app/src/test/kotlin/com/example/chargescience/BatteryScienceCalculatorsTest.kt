package com.example.chargescience

import com.example.chargescience.domain.science.BatteryScienceCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryScienceCalculatorsTest {

    @Test
    fun testInstantaneousPowerCalculation() {
        // Pixel 8a nominal: 3840 mV, 2200 mA -> 8.448 W
        val power = BatteryScienceCalculators.calculateInstantaneousPower(3840, 2200)
        assertEquals(8.448f, power, 0.001f)
    }

    @Test
    fun testPowerCalculationWithNegativeCurrent() {
        // Discharging at 500 mA, 3800 mV -> 1.90 W absolute net power
        val power = BatteryScienceCalculators.calculateInstantaneousPower(3800, -500)
        assertEquals(1.90f, power, 0.001f)
    }

    @Test
    fun testPowerCalculationWithSentinels() {
        // Sentinel Int.MIN_VALUE or zero voltage returns 0.0 W
        val p1 = BatteryScienceCalculators.calculateInstantaneousPower(3800, Int.MIN_VALUE)
        val p2 = BatteryScienceCalculators.calculateInstantaneousPower(0, 2000)
        val p3 = BatteryScienceCalculators.calculateInstantaneousPower(-100, 2000)

        assertEquals(0.0f, p1, 0.0f)
        assertEquals(0.0f, p2, 0.0f)
        assertEquals(0.0f, p3, 0.0f)
    }

    @Test
    fun testWeightedAveragePowerTrapezoidalIntegration() {
        // Non-uniform time steps:
        // t=0s, p=10W
        // t=10s, p=20W -> trapezoid energy = (10+20)/2 * 10 = 150 Joules
        // t=30s, p=20W -> trapezoid energy = 20 * 20 = 400 Joules
        // Total Joules = 550 J over 30s -> Average Power = 550 / 30 = 18.333 W
        val samples = listOf(
            Pair(0L, 10.0f),
            Pair(10_000L, 20.0f),
            Pair(30_000L, 20.0f)
        )
        val avgPower = BatteryScienceCalculators.calculateWeightedAveragePower(samples)
        assertEquals(18.333f, avgPower, 0.01f)
    }

    @Test
    fun testSessionConfidenceScoreDeepCharge() {
        // Optimal session: 15% -> 95% (80% swing), temp 25°C, reached full taper, hardware counter valid
        val score = BatteryScienceCalculators.calculateSessionConfidence(
            deltaSoc = 80,
            minTemp = 24.0f,
            maxTemp = 26.0f,
            avgTemp = 25.0f,
            maxThermalStatus = 0,
            reachedFullTaper = true,
            hardwareCounterValid = true
        )
        // Expected score: 0.40*100 + 0.25*100 + 0.20*100 + 0.15*100 = 100%
        assertTrue("Confidence score should exceed 90%", score >= 90.0f)
    }

    @Test
    fun testSessionConfidenceScoreShallowChargeRejection() {
        // Shallow charge: 40% -> 50% (10% swing)
        val score = BatteryScienceCalculators.calculateSessionConfidence(
            deltaSoc = 10,
            minTemp = 25.0f,
            maxTemp = 30.0f,
            avgTemp = 27.5f,
            maxThermalStatus = 0,
            reachedFullTaper = false,
            hardwareCounterValid = true
        )
        // Depth factor is 0, so confidence must be below the 85% threshold
        assertTrue("Shallow charge must yield low confidence", score < 60.0f)
    }

    @Test
    fun testFullChargeCapacityEstimation() {
        // 50% deltaSoc, charge counter delta = 2,243,000 uAh
        // Stored = 2,243,000 * 0.992 = 2,225,056 uAh
        // Estimated Full Cap = 2,225,056 / 0.50 = 4,450,112 uAh (~4,450 mAh)
        val capacityUah = BatteryScienceCalculators.estimateFullChargeCapacityUah(
            deltaSoc = 50,
            startCounterUah = 1_000_000L,
            endCounterUah = 3_243_000L
        )
        assertTrue("Estimated capacity should be near 4,450,000 uAh", capacityUah in 4_400_000L..4_500_000L)
    }
}
