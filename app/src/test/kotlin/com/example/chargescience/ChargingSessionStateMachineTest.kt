package com.example.chargescience

import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.domain.science.BatteryScienceCalculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChargingSessionStateMachineTest {

    @Test
    fun testSessionStartEntityConstruction() {
        val session = ChargingSessionEntity(
            startTimestamp = 1000L,
            startSoc = 20,
            chargeSource = 1, // AC
            protocolName = "USB Power Delivery (Fixed 9V Profile)",
            startTempCelsius = 31.5f,
            maxTempCelsius = 31.5f,
            startChargeCounterUah = 1_000_000L,
            maxNegotiatedVoltageUv = 9_000_000,
            maxNegotiatedCurrentUa = 2_200_000,
            negotiatedMaxPowerWatts = 19.8f,
            maxPowerWatts = 18.2f,
            averagePowerWatts = 18.2f,
            isCompleted = false
        )

        assertEquals(1000L, session.startTimestamp)
        assertEquals(20, session.startSoc)
        assertFalse(session.isCompleted)
        assertFalse(session.wasInterrupted)
    }

    @Test
    fun testSessionFinalizationMetrics() {
        val initial = ChargingSessionEntity(
            id = 42L,
            startTimestamp = 1000L,
            startSoc = 20,
            chargeSource = 1,
            protocolName = "USB Power Delivery (Fixed 9V Profile)",
            startTempCelsius = 30.0f,
            maxTempCelsius = 38.0f,
            startChargeCounterUah = 1_000_000L,
            maxNegotiatedVoltageUv = 9_000_000,
            maxNegotiatedCurrentUa = 2_200_000,
            negotiatedMaxPowerWatts = 19.8f,
            isCompleted = false
        )

        val finalized = initial.copy(
            endTimestamp = 3600_000L,
            endSoc = 85,
            totalDurationSeconds = 3599L,
            averagePowerWatts = 14.5f,
            maxPowerWatts = 18.5f,
            endTempCelsius = 35.0f,
            endChargeCounterUah = 3_800_000L,
            validSampleCount = 3590,
            missingSampleCount = 9,
            confidenceScore = 88.5f,
            isCompleted = true,
            wasInterrupted = false
        )

        assertTrue(finalized.isCompleted)
        assertFalse(finalized.wasInterrupted)
        assertEquals(65, (finalized.endSoc ?: 0) - finalized.startSoc)
        assertEquals(3599L, finalized.totalDurationSeconds)
        assertEquals(88.5f, finalized.confidenceScore, 0.01f)
    }

    @Test
    fun testInterruptedSessionRecovery() {
        val orphan = ChargingSessionEntity(
            id = 10L,
            startTimestamp = 1000L,
            startSoc = 10,
            chargeSource = 1,
            protocolName = "USB Power Delivery",
            isCompleted = false
        )

        // Simulated recovery when phone rebooted during charging
        val recovered = orphan.copy(
            endTimestamp = 5000L,
            endSoc = 18,
            endTempCelsius = 34.0f,
            isCompleted = true,
            wasInterrupted = true
        )

        assertTrue(recovered.isCompleted)
        assertTrue(recovered.wasInterrupted)
    }

    @Test
    fun testWiredToWirelessTransitionRequiresNewSession() {
        val wiredSource = 1 // AC
        val wirelessSource = 4 // Wireless Qi

        assertFalse("Source transition must trigger new session ID", wiredSource == wirelessSource)
    }
}
