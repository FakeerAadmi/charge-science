package com.example.chargescience

import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.domain.comparison.ChargingComparisonEngine
import com.example.chargescience.domain.comparison.ComparisonValidity
import org.junit.Assert.*
import org.junit.Test

class ChargingComparisonEngineTest {

    @Test
    fun testValidControlledComparison() {
        // Session A: 20% -> 80% (60% gain), 3000s, avg 15W, start 24°C, max 32°C
        val sessionA = ChargingSessionEntity(
            id = 1,
            startTimestamp = 1_000_000L,
            endTimestamp = 1_003_000_000L,
            startSoc = 20,
            endSoc = 80,
            chargeSource = 1, // AC Wired
            protocolName = "USB Power Delivery (9V)",
            totalDurationSeconds = 3000,
            averagePowerWatts = 15.0f,
            maxPowerWatts = 18.5f,
            startTempCelsius = 24.0f,
            maxTempCelsius = 32.0f,
            confidenceScore = 90.0f,
            isCompleted = true
        )

        // Session B: 22% -> 80% (58% gain), 3600s, avg 12W, start 25°C, max 31°C
        val sessionB = ChargingSessionEntity(
            id = 2,
            startTimestamp = 2_000_000L,
            endTimestamp = 2_003_600_000L,
            startSoc = 22,
            endSoc = 80,
            chargeSource = 1,
            protocolName = "USB Power Delivery (9V)",
            totalDurationSeconds = 3600,
            averagePowerWatts = 12.0f,
            maxPowerWatts = 15.0f,
            startTempCelsius = 25.0f,
            maxTempCelsius = 31.0f,
            confidenceScore = 88.0f,
            isCompleted = true
        )

        val result = ChargingComparisonEngine.compareSessions(sessionA, sessionB)

        assertEquals(ComparisonValidity.HIGH_FIDELITY_CONTROLLED, result.validity)
        assertTrue("No severe warnings for closely matched sessions", result.warnings.isEmpty())
        assertEquals(-600L, result.durationDeltaSeconds) // A was 600s faster
        assertEquals(3.0f, result.avgPowerDeltaWatts, 0.01f) // A had +3.0W average
        assertTrue(result.scientificVerdict.contains("Session A delivered higher sustained power"))
    }

    @Test
    fun testUncontrolledComparisonWithSocMismatch() {
        // Session A starts at 10%, Session B starts at 70% (60% delta!)
        val sessionA = ChargingSessionEntity(
            id = 1,
            startTimestamp = 1_000_000L,
            startSoc = 10,
            endSoc = 60,
            chargeSource = 1,
            protocolName = "USB Power Delivery",
            totalDurationSeconds = 1800,
            averagePowerWatts = 16.0f,
            startTempCelsius = 25.0f,
            maxTempCelsius = 33.0f,
            confidenceScore = 85.0f,
            isCompleted = true
        )

        val sessionB = ChargingSessionEntity(
            id = 2,
            startTimestamp = 2_000_000L,
            startSoc = 70,
            endSoc = 95,
            chargeSource = 1,
            protocolName = "USB Power Delivery",
            totalDurationSeconds = 1800,
            averagePowerWatts = 7.0f,
            startTempCelsius = 25.0f,
            maxTempCelsius = 29.0f,
            confidenceScore = 80.0f,
            isCompleted = true
        )

        val result = ChargingComparisonEngine.compareSessions(sessionA, sessionB)

        assertEquals(ComparisonValidity.UNCONTROLLED_MISLEADING, result.validity)
        assertTrue(result.warnings.any { it.contains("Starting SOC differs") })
        assertTrue(result.scientificVerdict.contains("Uncontrolled Comparison"))
    }

    @Test
    fun testWiredVsWirelessComparisonThermalLoad() {
        // Wired: 10 Wh delivered, 6°C rise -> 0.6 °C/Wh
        val wired = ChargingSessionEntity(
            id = 1,
            startTimestamp = 1_000_000L,
            startSoc = 20,
            endSoc = 70,
            chargeSource = 1, // AC Wired
            protocolName = "USB Power Delivery",
            totalDurationSeconds = 2400,
            averagePowerWatts = 15.0f,
            startTempCelsius = 25.0f,
            maxTempCelsius = 31.0f,
            confidenceScore = 90.0f,
            isCompleted = true
        )

        // Wireless: 10 Wh delivered (5W * 2h = 7200s), 14°C rise -> 1.4 °C/Wh
        val wireless = ChargingSessionEntity(
            id = 2,
            startTimestamp = 2_000_000L,
            startSoc = 22,
            endSoc = 72,
            chargeSource = 4, // Qi Wireless
            protocolName = "Qi Wireless",
            totalDurationSeconds = 7200,
            averagePowerWatts = 5.0f,
            startTempCelsius = 25.0f,
            maxTempCelsius = 39.0f,
            confidenceScore = 85.0f,
            isCompleted = true
        )

        val result = ChargingComparisonEngine.compareSessions(wired, wireless)

        assertTrue(result.thermalLoadCoeffB > result.thermalLoadCoeffA * 2.0f)
        assertTrue(result.scientificVerdict.contains("Wired vs Wireless"))
    }
}
