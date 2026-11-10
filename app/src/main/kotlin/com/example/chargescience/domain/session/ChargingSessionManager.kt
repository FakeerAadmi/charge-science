package com.example.chargescience.domain.session

import android.os.BatteryManager
import android.util.Log
import com.example.chargescience.data.db.AppDatabase
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.diagnostics.BatteryTelemetryCollector
import com.example.chargescience.domain.science.BatteryScienceCalculators
import com.example.chargescience.model.BatteryTelemetry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max

/**
 * Robust charging session state machine.
 * Detects:
 * - Charging started
 * - Charging stopped / unplugged
 * - Charging resumed
 * - Charging method / protocol changed
 * - Battery reached 100%
 * - Process interruption / reboot recovery
 * - Samples & persists 1 Hz telemetry
 */
class ChargingSessionManager(
    private val database: AppDatabase,
    private val telemetryCollector: BatteryTelemetryCollector,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val sessionDao = database.chargingSessionDao()
    private val sampleDao = database.telemetrySampleDao()

    @Volatile
    private var currentSessionId: Long? = null

    @Volatile
    private var currentChargeSource: Int = 0

    @Volatile
    private var isActivelyCharging: Boolean = false

    // In-memory accumulation for active session
    private val sessionPowerSamples = mutableListOf<Pair<Long, Float>>()
    private var peakPowerWatts: Float = 0f
    private var maxCurrentMa: Int = 0
    private var currentSumMa: Long = 0L
    private var minTemperatureC: Float = Float.MAX_VALUE
    private var maxTemperatureC: Float = Float.MIN_VALUE
    private var throttleEventCount: Int = 0
    private var validSampleCount: Int = 0
    private var missingSampleCount: Int = 0
    private var activeChargingSeconds: Long = 0L
    private var lastSampleTimeMs: Long = 0L

    private val _activeSessionFlow = MutableStateFlow<ChargingSessionEntity?>(null)
    val activeSessionFlow: StateFlow<ChargingSessionEntity?> = _activeSessionFlow.asStateFlow()

    init {
        // Step 1: Recover any orphaned/interrupted sessions on app start
        scope.launch {
            recoverInterruptedSessions()
        }

        // Step 2: Observe live telemetry flow
        scope.launch {
            telemetryCollector.telemetry.collect { telemetry ->
                handleTelemetryTick(telemetry)
            }
        }
    }

    private suspend fun recoverInterruptedSessions() {
        try {
            val incompleteSessions = sessionDao.getIncompleteSessions()
            for (session in incompleteSessions) {
                val latestSample = sampleDao.getLatestSampleForSession(session.id)
                val resolvedEndTime = latestSample?.timestamp ?: (session.startTimestamp + 1000L)
                val resolvedEndSoc = latestSample?.batteryLevel ?: session.startSoc
                val resolvedEndTemp = latestSample?.temperatureCelsius ?: session.startTempCelsius

                val recovered = session.copy(
                    endTimestamp = resolvedEndTime,
                    endSoc = resolvedEndSoc,
                    endTempCelsius = resolvedEndTemp,
                    isCompleted = true,
                    wasInterrupted = true
                )
                sessionDao.updateSession(recovered)
                Log.w("ChargeScience", "Recovered interrupted session #${session.id} with endTimestamp=$resolvedEndTime")
            }
        } catch (e: Exception) {
            Log.e("ChargeScience", "Error recovering interrupted sessions", e)
        }
    }

    private suspend fun handleTelemetryTick(telemetry: BatteryTelemetry) {
        val plugged = telemetry.pluggedSource
        val isChargingNow = telemetry.isCharging && plugged != 0

        // Case 1: Device was unplugged
        if (!isChargingNow && currentSessionId != null) {
            Log.i("ChargeScience", "Charging stopped / unplugged. Finalizing session #$currentSessionId")
            finalizeCurrentSession(telemetry, wasInterrupted = false)
            return
        }

        // Case 2: Plugged in, new session started
        if (isChargingNow && currentSessionId == null) {
            Log.i("ChargeScience", "Charging started (source=$plugged, protocol=${telemetry.protocolName})")
            startNewSession(telemetry)
            return
        }

        // Case 3: Charging method changed while plugged (e.g., switched from AC to USB or Wireless)
        if (isChargingNow && currentSessionId != null && plugged != currentChargeSource) {
            Log.i("ChargeScience", "Charging method changed from $currentChargeSource to $plugged. Rollover session.")
            finalizeCurrentSession(telemetry, wasInterrupted = false)
            startNewSession(telemetry)
            return
        }

        // Case 4: Active session ongoing - record sample
        if (isChargingNow && currentSessionId != null) {
            recordActiveSample(telemetry)
        }
    }

    private suspend fun startNewSession(telemetry: BatteryTelemetry) {
        val now = System.currentTimeMillis()
        currentChargeSource = telemetry.pluggedSource
        isActivelyCharging = true

        // Reset accumulators
        sessionPowerSamples.clear()
        peakPowerWatts = telemetry.wattage
        maxCurrentMa = telemetry.currentMa
        currentSumMa = telemetry.currentMa.toLong()
        minTemperatureC = telemetry.temperatureCelsius
        maxTemperatureC = telemetry.temperatureCelsius
        throttleEventCount = if (telemetry.thermalStatus >= 2) 1 else 0
        validSampleCount = 1
        missingSampleCount = 0
        activeChargingSeconds = 0L
        lastSampleTimeMs = now

        sessionPowerSamples.add(Pair(now, telemetry.wattage))

        val entity = ChargingSessionEntity(
            startTimestamp = now,
            startSoc = telemetry.level,
            chargeSource = telemetry.pluggedSource,
            protocolName = telemetry.protocolName,
            startTempCelsius = telemetry.temperatureCelsius,
            maxTempCelsius = telemetry.temperatureCelsius,
            startChargeCounterUah = telemetry.chargeCounterUah,
            maxNegotiatedVoltageUv = telemetry.maxChargingVoltageUv,
            maxNegotiatedCurrentUa = telemetry.maxChargingCurrentUa,
            negotiatedMaxPowerWatts = telemetry.negotiatedMaxPowerWatts,
            maxPowerWatts = telemetry.wattage,
            averagePowerWatts = telemetry.wattage,
            maxCurrentMa = telemetry.currentMa,
            averageCurrentMa = telemetry.currentMa,
            validSampleCount = 1,
            isCompleted = false
        )

        val id = sessionDao.insertSession(entity)
        currentSessionId = id
        _activeSessionFlow.value = entity.copy(id = id)

        // Persist initial sample
        sampleDao.insertSample(
            TelemetrySampleEntity(
                sessionId = id,
                timestamp = now,
                voltageMv = telemetry.voltageMv,
                currentMa = telemetry.currentMa,
                powerWatts = telemetry.wattage,
                temperatureCelsius = telemetry.temperatureCelsius,
                batteryLevel = telemetry.level,
                thermalStatus = telemetry.thermalStatus
            )
        )
    }

    private suspend fun recordActiveSample(telemetry: BatteryTelemetry) {
        val sessionId = currentSessionId ?: return
        val now = System.currentTimeMillis()

        // Check if sample is valid
        val isValid = telemetry.voltageMv > 0 && telemetry.currentMa != Int.MIN_VALUE
        if (isValid) {
            validSampleCount++
            peakPowerWatts = max(peakPowerWatts, telemetry.wattage)
            maxCurrentMa = max(maxCurrentMa, telemetry.currentMa)
            currentSumMa += telemetry.currentMa
            minTemperatureC = minOf(minTemperatureC, telemetry.temperatureCelsius)
            maxTemperatureC = maxOf(maxTemperatureC, telemetry.temperatureCelsius)
            if (telemetry.thermalStatus >= 2) {
                throttleEventCount++
            }
            sessionPowerSamples.add(Pair(now, telemetry.wattage))

            // Time difference since last sample
            val dtSec = if (lastSampleTimeMs > 0) (now - lastSampleTimeMs) / 1000L else 1L
            if (dtSec in 1..5) {
                activeChargingSeconds += dtSec
            } else {
                activeChargingSeconds += 1L
            }
            lastSampleTimeMs = now

            // Persist sample to Room
            sampleDao.insertSample(
                TelemetrySampleEntity(
                    sessionId = sessionId,
                    timestamp = now,
                    voltageMv = telemetry.voltageMv,
                    currentMa = telemetry.currentMa,
                    powerWatts = telemetry.wattage,
                    temperatureCelsius = telemetry.temperatureCelsius,
                    batteryLevel = telemetry.level,
                    thermalStatus = telemetry.thermalStatus
                )
            )

            // Periodically update active session entity in memory
            val avgPower = BatteryScienceCalculators.calculateWeightedAveragePower(sessionPowerSamples)
            val avgCurrent = if (validSampleCount > 0) (currentSumMa / validSampleCount).toInt() else telemetry.currentMa

            val updated = _activeSessionFlow.value?.copy(
                totalDurationSeconds = activeChargingSeconds,
                maxPowerWatts = peakPowerWatts,
                averagePowerWatts = avgPower,
                maxCurrentMa = maxCurrentMa,
                averageCurrentMa = avgCurrent,
                maxTempCelsius = maxTemperatureC,
                validSampleCount = validSampleCount,
                missingSampleCount = missingSampleCount
            )
            _activeSessionFlow.value = updated
        } else {
            missingSampleCount++
        }
    }

    private suspend fun finalizeCurrentSession(telemetry: BatteryTelemetry, wasInterrupted: Boolean) {
        val sessionId = currentSessionId ?: return
        val now = System.currentTimeMillis()

        val avgPower = BatteryScienceCalculators.calculateWeightedAveragePower(sessionPowerSamples)
        val avgCurrent = if (validSampleCount > 0) (currentSumMa / validSampleCount).toInt() else 0

        val existingSession = sessionDao.getSessionById(sessionId)
        val startSoc = existingSession?.startSoc ?: telemetry.level
        val deltaSoc = telemetry.level - startSoc

        val reachedFullTaper = telemetry.level >= 100 || telemetry.status == BatteryManager.BATTERY_STATUS_FULL

        val avgTemp = if (validSampleCount > 0) (minTemperatureC + maxTemperatureC) / 2.0f else telemetry.temperatureCelsius
        val confidence = BatteryScienceCalculators.calculateSessionConfidence(
            deltaSoc = deltaSoc,
            minTemp = if (minTemperatureC == Float.MAX_VALUE) telemetry.temperatureCelsius else minTemperatureC,
            maxTemp = if (maxTemperatureC == Float.MIN_VALUE) telemetry.temperatureCelsius else maxTemperatureC,
            avgTemp = avgTemp,
            maxThermalStatus = if (throttleEventCount > 0) 2 else 0,
            reachedFullTaper = reachedFullTaper,
            hardwareCounterValid = telemetry.chargeCounterUah > 0
        )

        val finalized = ChargingSessionEntity(
            id = sessionId,
            startTimestamp = existingSession?.startTimestamp ?: (now - activeChargingSeconds * 1000L),
            endTimestamp = now,
            startSoc = startSoc,
            endSoc = telemetry.level,
            chargeSource = currentChargeSource,
            protocolName = existingSession?.protocolName ?: telemetry.protocolName,
            totalDurationSeconds = activeChargingSeconds,
            averagePowerWatts = avgPower,
            maxPowerWatts = peakPowerWatts,
            averageCurrentMa = avgCurrent,
            maxCurrentMa = maxCurrentMa,
            startTempCelsius = existingSession?.startTempCelsius ?: telemetry.temperatureCelsius,
            maxTempCelsius = if (maxTemperatureC == Float.MIN_VALUE) telemetry.temperatureCelsius else maxTemperatureC,
            endTempCelsius = telemetry.temperatureCelsius,
            startChargeCounterUah = existingSession?.startChargeCounterUah ?: 0L,
            endChargeCounterUah = telemetry.chargeCounterUah,
            maxNegotiatedVoltageUv = existingSession?.maxNegotiatedVoltageUv ?: telemetry.maxChargingVoltageUv,
            maxNegotiatedCurrentUa = existingSession?.maxNegotiatedCurrentUa ?: telemetry.maxChargingCurrentUa,
            negotiatedMaxPowerWatts = existingSession?.negotiatedMaxPowerWatts ?: telemetry.negotiatedMaxPowerWatts,
            thermalThrottleCount = throttleEventCount,
            validSampleCount = validSampleCount,
            missingSampleCount = missingSampleCount,
            confidenceScore = confidence,
            isCompleted = true,
            wasInterrupted = wasInterrupted
        )

        sessionDao.updateSession(finalized)
        Log.i("ChargeScience", "Session #$sessionId finalized: duration=${activeChargingSeconds}s, avgPower=${avgPower}W, deltaSoc=${deltaSoc}%, confidence=${confidence}%")

        currentSessionId = null
        currentChargeSource = 0
        isActivelyCharging = false
        _activeSessionFlow.value = null
    }
}
