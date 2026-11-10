package com.example.chargescience

import com.example.chargescience.data.db.entity.BatteryCapacityEstimateEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.domain.health.BatteryHealthLaboratoryEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BatteryHealthLaboratoryEngineTest {

    private lateinit var engine: BatteryHealthLaboratoryEngine

    @Before
    fun setUp() {
        engine = BatteryHealthLaboratoryEngine()
    }

    private fun createSession(
        startSoc: Int,
        endSoc: Int,
        startChargeUah: Long,
        endChargeUah: Long,
        startTemp: Float = 28.0f,
        maxTemp: Float = 33.0f,
        endTemp: Float = 31.0f,
        wasInterrupted: Boolean = false,
        confidenceScore: Float = 0.95f
    ): ChargingSessionEntity {
        return ChargingSessionEntity(
            id = 1L,
            startTimestamp = 1000000L,
            endTimestamp = 1003600L,
            startSoc = startSoc,
            endSoc = endSoc,
            chargeSource = 1, // AC
            protocolName = "USB Power Delivery",
            totalDurationSeconds = 3600L,
            averagePowerWatts = 12.0f,
            maxPowerWatts = 18.0f,
            averageCurrentMa = 2800,
            maxCurrentMa = 4200,
            startTempCelsius = startTemp,
            maxTempCelsius = maxTemp,
            endTempCelsius = endTemp,
            startChargeCounterUah = startChargeUah,
            endChargeCounterUah = endChargeUah,
            maxNegotiatedVoltageUv = 9000000,
            maxNegotiatedCurrentUa = 2000000,
            negotiatedMaxPowerWatts = 18.0f,
            thermalThrottleCount = 0,
            validSampleCount = 3600,
            missingSampleCount = 0,
            confidenceScore = confidenceScore,
            isCompleted = true,
            wasInterrupted = wasInterrupted
        )
    }

    @Test
    fun testValidChargingSessionCapacityCalculation() {
        // 20% to 70% (ΔSOC = 50%)
        // Real capacity: 4,200 mAh -> ΔQ = 50% * 4,200 = 2,100 mAh = 2,100,000 µAh
        val session = createSession(
            startSoc = 20,
            endSoc = 70,
            startChargeUah = 800_000L,
            endChargeUah = 2_900_000L // ΔQ = 2,100,000 µAh
        )

        val estimate = engine.evaluateSessionCapacity(session)

        assertEquals("VALID", estimate.eligibilityStatus)
        assertNull(estimate.rejectionReason)
        assertEquals(4200.0f, estimate.calculatedCapacityMah, 1.0f)
        // SoH against 4,492 mAh nominal = 4200 / 4492 * 100 = 93.5%
        assertEquals(93.5f, estimate.estimatedSohPercent, 0.2f)
    }

    @Test
    fun testRejectsLowDeltaSocSession() {
        // 50% to 65% (ΔSOC = 15% < 30%)
        val session = createSession(
            startSoc = 50,
            endSoc = 65,
            startChargeUah = 2_000_000L,
            endChargeUah = 2_600_000L
        )

        val estimate = engine.evaluateSessionCapacity(session)

        assertEquals("REJECTED_LOW_DELTA_SOC", estimate.eligibilityStatus)
        assertNotNull(estimate.rejectionReason)
        assertTrue(estimate.rejectionReason!!.contains("Quantization error"))
    }

    @Test
    fun testRejectsThermalInstabilitySession() {
        // Temperature rises from 28°C to 46.5°C (ΔT = 18.5°C > 8°C and peak > 45°C)
        val session = createSession(
            startSoc = 20,
            endSoc = 80,
            startChargeUah = 800_000L,
            endChargeUah = 3_200_000L,
            startTemp = 28.0f,
            maxTemp = 46.5f
        )

        val estimate = engine.evaluateSessionCapacity(session)

        assertEquals("REJECTED_THERMAL_INSTABILITY", estimate.eligibilityStatus)
        assertNotNull(estimate.rejectionReason)
        assertTrue(estimate.rejectionReason!!.contains("distorts cell capacity"))
    }

    @Test
    fun testRejectsInterruptedSession() {
        val session = createSession(
            startSoc = 20,
            endSoc = 80,
            startChargeUah = 800_000L,
            endChargeUah = 3_200_000L,
            wasInterrupted = true,
            confidenceScore = 0.50f
        )

        val estimate = engine.evaluateSessionCapacity(session)

        assertEquals("REJECTED_INTERRUPTED", estimate.eligibilityStatus)
        assertNotNull(estimate.rejectionReason)
    }

    @Test
    fun testMultiSessionHealthSummaryComputation() {
        val estimates = listOf(
            BatteryCapacityEstimateEntity(
                sessionId = 1L,
                timestamp = 1000L,
                startSoc = 20,
                endSoc = 70,
                deltaSoc = 50,
                deltaChargeCounterUah = 2_100_000L,
                calculatedCapacityMah = 4200f,
                estimatedSohPercent = 93.5f,
                averageTempCelsius = 30.0f,
                maxTempCelsius = 33.0f,
                cycleCountAtSession = 780,
                eligibilityStatus = "VALID"
            ),
            BatteryCapacityEstimateEntity(
                sessionId = 2L,
                timestamp = 2000L,
                startSoc = 15,
                endSoc = 75,
                deltaSoc = 60,
                deltaChargeCounterUah = 2_520_000L,
                calculatedCapacityMah = 4200f,
                estimatedSohPercent = 93.5f,
                averageTempCelsius = 31.0f,
                maxTempCelsius = 34.0f,
                cycleCountAtSession = 782,
                eligibilityStatus = "VALID"
            ),
            BatteryCapacityEstimateEntity(
                sessionId = 3L,
                timestamp = 3000L,
                startSoc = 50,
                endSoc = 60,
                deltaSoc = 10,
                deltaChargeCounterUah = 400_000L,
                calculatedCapacityMah = 4000f,
                estimatedSohPercent = 89.0f,
                averageTempCelsius = 31.0f,
                maxTempCelsius = 34.0f,
                cycleCountAtSession = 782,
                eligibilityStatus = "REJECTED_LOW_DELTA_SOC"
            )
        )

        val summary = engine.computeHealthSummary(
            hardwareCycleCount = 782,
            allEstimates = estimates,
            allSessions = emptyList()
        )

        assertEquals(782, summary.hardwareCycleCount)
        assertEquals(4492, summary.nominalDesignCapacityMah)
        assertEquals(4200f, summary.latestLearnedCapacityMah!!, 5.0f)
        assertEquals(93.5f, summary.estimatedSohPercent!!, 0.5f)
        assertEquals(2, summary.validObservationCount)
        assertTrue(summary.confidenceScore >= 0.70f)
    }
}
