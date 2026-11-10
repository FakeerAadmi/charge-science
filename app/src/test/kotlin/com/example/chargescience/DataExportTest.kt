package com.example.chargescience

import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import com.example.chargescience.domain.export.DataExportManager
import org.junit.Assert.*
import org.junit.Test

class DataExportTest {

    @Test
    fun testTelemetrySamplesCsvExport() {
        val session = ChargingSessionEntity(
            id = 42,
            startTimestamp = 1_700_000_000_000L,
            startSoc = 25,
            chargeSource = 1,
            protocolName = "USB Power Delivery (Fixed 9V)"
        )

        val samples = listOf(
            TelemetrySampleEntity(
                sampleId = 1,
                sessionId = 42,
                timestamp = 1_700_000_000_000L,
                voltageMv = 3850,
                currentMa = 2200,
                powerWatts = 8.47f,
                temperatureCelsius = 26.5f,
                batteryLevel = 25,
                thermalStatus = 0
            ),
            TelemetrySampleEntity(
                sampleId = 2,
                sessionId = 42,
                timestamp = 1_700_000_001_000L,
                voltageMv = 3855,
                currentMa = 2210,
                powerWatts = 8.52f,
                temperatureCelsius = 26.6f,
                batteryLevel = 25,
                thermalStatus = 0
            )
        )

        val csv = DataExportManager.exportTelemetrySamplesCsv(session, samples)

        assertTrue(csv.contains("sample_id,session_id,timestamp_epoch_ms"))
        assertTrue(csv.contains("1,42,1700000000000,"))
        assertTrue(csv.contains("3850,2200,8.470,26.5,25,0"))
        assertTrue(csv.contains("3855,2210,8.520,26.6,25,0"))
    }

    @Test
    fun testSessionsCsvExportWithAccessoryEscaping() {
        val session = ChargingSessionEntity(
            id = 10,
            startTimestamp = 1_700_000_000_000L,
            endTimestamp = 1_700_001_800_000L,
            startSoc = 30,
            endSoc = 80,
            chargeSource = 1,
            protocolName = "USB Power Delivery, 9V Profile", // Contains comma, requires RFC 4180 quotes
            totalDurationSeconds = 1800,
            averagePowerWatts = 14.5f,
            maxPowerWatts = 18.2f,
            confidenceScore = 92.5f,
            accessoryId = 101
        )

        val accessories = mapOf(
            101L to AccessoryProfileEntity(
                id = 101,
                type = "CHARGER",
                nickname = "Anker \"Nano II\" 30W, Type-C", // Contains quotes and comma!
                claimedWattage = 30f
            )
        )

        val csv = DataExportManager.exportSessionsCsv(listOf(session), accessories)

        // Verifies header
        assertTrue(csv.contains("session_id,start_timestamp_epoch_ms"))
        // Verifies quoted protocol name
        assertTrue(csv.contains("\"USB Power Delivery, 9V Profile\""))
        // Verifies RFC 4180 escaped quotes for accessory name
        assertTrue(csv.contains("\"Anker \"\"Nano II\"\" 30W, Type-C\""))
    }

    @Test
    fun testCompleteJsonExportStructure() {
        val session = ChargingSessionEntity(
            id = 1,
            startTimestamp = 1_700_000_000_000L,
            endTimestamp = 1_700_000_060_000L,
            startSoc = 50,
            endSoc = 52,
            chargeSource = 1,
            protocolName = "USB-PD",
            totalDurationSeconds = 60,
            averagePowerWatts = 12.0f,
            maxPowerWatts = 15.0f,
            confidenceScore = 75.0f
        )

        val sample = TelemetrySampleEntity(
            sampleId = 1,
            sessionId = 1,
            timestamp = 1_700_000_000_000L,
            voltageMv = 3900,
            currentMa = 2000,
            powerWatts = 7.8f,
            temperatureCelsius = 28.0f,
            batteryLevel = 50,
            thermalStatus = 0
        )

        val accessory = AccessoryProfileEntity(
            id = 1,
            type = "CHARGER",
            nickname = "Google 30W USB-C",
            claimedWattage = 30f
        )

        val json = DataExportManager.exportCompleteJson(
            sessionsWithSamples = listOf(Pair(session, listOf(sample))),
            accessories = listOf(accessory)
        )

        assertTrue(json.contains("\"app_name\": \"Charge Science\""))
        assertTrue(json.contains("\"nominal_design_capacity_mah\": 4492"))
        assertTrue(json.contains("\"nickname\": \"Google 30W USB-C\""))
        assertTrue(json.contains("\"session_id\": 1"))
        assertTrue(json.contains("\"samples\": ["))
        assertTrue(json.contains("\"v\":3900"))
    }
}
