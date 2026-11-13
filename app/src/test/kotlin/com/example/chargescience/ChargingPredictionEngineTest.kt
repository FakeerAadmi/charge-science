package com.example.chargescience

import com.example.chargescience.domain.prediction.ChargingPredictionEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class ChargingPredictionEngineTest {

    private lateinit var engine: ChargingPredictionEngine

    @Before
    fun setUp() {
        engine = ChargingPredictionEngine()
    }

    @Test
    fun testWiredVsWirelessPredictionAtLowSoc() {
        val forecast = engine.predictCharging(currentSoc = 20)

        // At 20% SOC: Wired full charge is ~60-70 mins; Wireless is ~170-200 mins
        assertTrue("Wired should be faster than wireless", forecast.wired.timeToFullMinutes < forecast.wireless.timeToFullMinutes)
        assertTrue("Wired time should be reasonable (50-80m)", forecast.wired.timeToFullMinutes in 50..85)
        assertTrue("Wireless time should be reasonable (150-220m)", forecast.wireless.timeToFullMinutes in 150..220)

        // Time to 80% should be strictly less than time to 100%
        assertTrue(forecast.wired.timeTo80Minutes < forecast.wired.timeToFullMinutes)
        assertTrue(forecast.wireless.timeTo80Minutes < forecast.wireless.timeToFullMinutes)

        // Energy: Wireless requires ~35-40% more grid energy due to induction losses
        assertTrue("Wireless wall energy should exceed wired", forecast.wireless.wallEnergyWh > forecast.wired.wallEnergyWh)
        assertTrue(abs(37 - forecast.energyOverheadPercent) <= 5)

        // Peak power ratings
        assertEquals(18.0f, forecast.wired.peakPowerWatts, 0.1f)
        assertEquals(7.5f, forecast.wireless.peakPowerWatts, 0.1f)
    }

    @Test
    fun testFullyChargedBatteryReturnsZero() {
        val forecast = engine.predictCharging(currentSoc = 100)

        assertEquals(0, forecast.wired.timeToFullMinutes)
        assertEquals(0, forecast.wired.timeTo80Minutes)
        assertEquals(0.0f, forecast.wired.wallEnergyWh, 0.01f)

        assertEquals(0, forecast.wireless.timeToFullMinutes)
        assertEquals(0, forecast.wireless.timeTo80Minutes)
        assertEquals(0.0f, forecast.wireless.wallEnergyWh, 0.01f)
    }

    @Test
    fun testTaperPhaseAtHighSoc() {
        // At 85% SOC: battery is already past 80%
        val forecast = engine.predictCharging(currentSoc = 85)

        assertEquals(0, forecast.wired.timeTo80Minutes)
        assertEquals(0, forecast.wireless.timeTo80Minutes)

        // Should still take ~10-35m in CV saturation taper to top off from 85% to 100%
        assertTrue("Wired taper should take 10-35m", forecast.wired.timeToFullMinutes in 10..35)
        assertTrue("Wireless taper should take 15-40m", forecast.wireless.timeToFullMinutes in 15..40)
    }

    @Test
    fun testEnduranceForecastingAtFullCharge() {
        val endurance = engine.predictEndurance(currentSoc = 100, usableCapacityMah = 4492)

        // SOT mixed (480mA) = 4492 / 480 * 60 ≈ 561 min (9h 21m)
        assertTrue(abs(561 - endurance.activeMixedSotMinutes) <= 5)
        assertTrue(endurance.activeMixedSotFormatted.contains("9h"))

        // Light (350mA) = 4492 / 350 * 60 ≈ 770 min (12h 50m)
        assertTrue(endurance.lightSotMinutes > endurance.activeMixedSotMinutes)
        assertTrue(endurance.lightSotFormatted.contains("12h"))

        // Heavy (850mA) = 4492 / 850 * 60 ≈ 317 min (5h 17m)
        assertTrue(endurance.heavySotMinutes < endurance.activeMixedSotMinutes)
        assertTrue(endurance.heavySotFormatted.contains("5h"))

        // Standby (18mA) = 4492 / 18 ≈ 249 hours (10.4 days)
        assertTrue(endurance.standbyHours >= 200f)
        assertTrue(endurance.standbyFormatted.contains("days"))
    }

    @Test
    fun testEnduranceForecastingScalesWithSoc() {
        val full = engine.predictEndurance(currentSoc = 100)
        val half = engine.predictEndurance(currentSoc = 50)

        // Half SOC should provide approximately half the minutes
        assertTrue(abs(full.activeMixedSotMinutes / 2 - half.activeMixedSotMinutes) <= 2)
        assertTrue(abs(full.lightSotMinutes / 2 - half.lightSotMinutes) <= 2)
        assertTrue(abs(full.heavySotMinutes / 2 - half.heavySotMinutes) <= 2)
    }
}
