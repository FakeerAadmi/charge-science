package com.example.chargescience

import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.domain.wireless.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class WirelessDiagnosticEngineTest {

    private lateinit var engine: WirelessDiagnosticEngine

    @Before
    fun setUp() {
        engine = WirelessDiagnosticEngine()
    }

    private fun generateSamples(
        count: Int,
        startTs: Long,
        baseWattage: Float,
        tempClimbRate: Float,
        isGradualDrop: Boolean = false,
        isOscillating: Boolean = false
    ): List<TelemetrySampleEntity> {
        val samples = mutableListOf<TelemetrySampleEntity>()
        var currentTemp = 32.0f

        for (i in 0 until count) {
            val ts = startTs + (i * 1000L)
            currentTemp += tempClimbRate

            val power = when {
                isOscillating -> if (i % 2 == 0) 1.2f else 6.5f
                isGradualDrop && i >= (count - 15) -> {
                    val step = (count - 1 - i).toFloat()
                    baseWattage * (step / 15f)
                }
                else -> baseWattage
            }

            samples.add(
                TelemetrySampleEntity(
                    sampleId = i.toLong(),
                    sessionId = 1L,
                    timestamp = ts,
                    voltageMv = 4100,
                    currentMa = (power / 4.1f * 1000).toInt(),
                    powerWatts = power,
                    temperatureCelsius = currentTemp,
                    batteryLevel = 30 + (i / 100),
                    thermalStatus = 0
                )
            )
        }
        return samples
    }

    @Test
    fun testStableCompletionReturnsStableOutcome() {
        val startTs = 1000000L
        val samples = generateSamples(count = 100, startTs = startTs, baseWattage = 4.5f, tempClimbRate = 0.05f)
        val ring = MagneticRingProfile()

        val result = engine.analyzeInterruption(
            experimentType = ExperimentType.BASELINE_A,
            ringProfile = ring,
            samples = samples,
            startTimestamp = startTs,
            interruptionTimestamp = null,
            startTempCelsius = 32.0f,
            maxTempCelsius = 37.0f,
            maxThermalStatus = 0,
            didDisconnect = false
        )

        assertEquals(DiagnosticOutcomeType.STABLE_COMPLETION, result.outcomeType)
        assertEquals("NONE", result.dropPattern)
        assertTrue(result.confidenceScore >= 0.90f)
    }

    @Test
    fun testModeAPhoneThermalThrottleCutoff() {
        val startTs = 1000000L
        val count = 120
        val samples = generateSamples(
            count = count,
            startTs = startTs,
            baseWattage = 5.0f,
            tempClimbRate = 0.08f,
            isGradualDrop = true
        )
        val interruptionTs = startTs + (count * 1000L)
        val ring = MagneticRingProfile(offsetFromCoilCenterMm = 7.8f, isPermanentMagnet = false)

        val result = engine.analyzeInterruption(
            experimentType = ExperimentType.BASELINE_A,
            ringProfile = ring,
            samples = samples,
            startTimestamp = startTs,
            interruptionTimestamp = interruptionTs,
            startTempCelsius = 32.0f,
            maxTempCelsius = 41.6f,
            maxThermalStatus = 2,
            didDisconnect = true
        )

        assertEquals(DiagnosticOutcomeType.PHONE_THERMAL_THROTTLE, result.outcomeType)
        assertEquals("GRADUAL_THROTTLE", result.dropPattern)
        assertTrue(result.confidenceScore >= 0.85f)
        assertTrue(result.supportingEvidence.any { it.contains("VIRTUAL-SKIN-CHARGE-WLC") })
    }

    @Test
    fun testModeBTransmitterFodOrPadOverheat() {
        val startTs = 1000000L
        val count = 100
        val samples = generateSamples(
            count = count,
            startTs = startTs,
            baseWattage = 5.0f,
            tempClimbRate = 0.03f,
            isGradualDrop = false
        )
        val interruptionTs = startTs + (count * 1000L)
        val ring = MagneticRingProfile(isPermanentMagnet = true)

        val result = engine.analyzeInterruption(
            experimentType = ExperimentType.BASELINE_A,
            ringProfile = ring,
            samples = samples,
            startTimestamp = startTs,
            interruptionTimestamp = interruptionTs,
            startTempCelsius = 32.0f,
            maxTempCelsius = 35.0f,
            maxThermalStatus = 0,
            didDisconnect = true
        )

        assertEquals(DiagnosticOutcomeType.TX_SHUTDOWN_FOD, result.outcomeType)
        assertEquals("ABRUPT", result.dropPattern)
        assertTrue(result.supportingEvidence.any { it.contains("remained cool") })
    }

    @Test
    fun testPowerCouplingCollapseFromMisalignment() {
        val startTs = 1000000L
        val count = 100
        val samples = generateSamples(
            count = count,
            startTs = startTs,
            baseWattage = 4.0f,
            tempClimbRate = 0.02f,
            isOscillating = true
        )
        val interruptionTs = startTs + (count * 1000L)
        val ring = MagneticRingProfile(offsetFromCoilCenterMm = 10.0f)

        val result = engine.analyzeInterruption(
            experimentType = ExperimentType.ALIGNMENT_B,
            ringProfile = ring,
            samples = samples,
            startTimestamp = startTs,
            interruptionTimestamp = interruptionTs,
            startTempCelsius = 30.0f,
            maxTempCelsius = 32.0f,
            maxThermalStatus = 0,
            didDisconnect = true
        )

        assertEquals(DiagnosticOutcomeType.POWER_COUPLING_COLLAPSE, result.outcomeType)
        assertEquals("OSCILLATING", result.dropPattern)
        assertTrue(result.recommendedNextExperiment.contains("Experiment B"))
    }

    @Test
    fun testMagneticRingProfileRiskTiers() {
        val criticalRing = MagneticRingProfile(
            innerDiameterMm = 42.0f,
            offsetFromCoilCenterMm = 7.8f,
            isPermanentMagnet = false
        )
        assertTrue(criticalRing.hasApertureOverlapRisk)
        assertEquals(EddyRiskTier.CRITICAL_UNSEGMENTED_METAL_IN_FLUX, criticalRing.eddyCurrentRiskTier)

        val safeRing = MagneticRingProfile(
            innerDiameterMm = 46.0f,
            offsetFromCoilCenterMm = 1.0f,
            isPermanentMagnet = true
        )
        assertFalse(safeRing.hasApertureOverlapRisk)
        assertEquals(EddyRiskTier.LOW_ALIGNED_SEGMENTED, safeRing.eddyCurrentRiskTier)
    }
}
