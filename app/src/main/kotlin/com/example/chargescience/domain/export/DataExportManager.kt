package com.example.chargescience.domain.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.chargescience.data.db.entity.AccessoryProfileEntity
import com.example.chargescience.data.db.entity.ChargingSessionEntity
import com.example.chargescience.data.db.entity.TelemetrySampleEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * High-performance, RFC 4180 compliant local data export engine.
 * Formats:
 * 1. Telemetry Samples CSV (time-series points)
 * 2. Charging Sessions CSV (aggregated sessions)
 * 3. Complete Diagnostic JSON (self-contained structured laboratory export)
 *
 * Operates strictly locally without any network permissions.
 */
object DataExportManager {

    private const val SCHEMA_VERSION = 1
    private const val APP_VERSION = "1.0"
    private const val NOMINAL_CAPACITY_MAH = 4492

    private fun getUtcIsoFormat(): SimpleDateFormat {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    private fun escapeCsv(value: Any?): String {
        val str = value?.toString() ?: ""
        return if (str.contains(",") || str.contains("\"") || str.contains("\n") || str.contains("\r")) {
            "\"" + str.replace("\"", "\"\"") + "\""
        } else {
            str
        }
    }

    /**
     * Exports raw 1 Hz telemetry samples for a specific charging session into RFC 4180 CSV.
     */
    fun exportTelemetrySamplesCsv(
        session: ChargingSessionEntity,
        samples: List<TelemetrySampleEntity>
    ): String {
        val isoFormat = getUtcIsoFormat()
        val sb = StringBuilder()

        // Metadata Header Comment
        sb.append("# Charge Science Telemetry Export\n")
        sb.append("# Session ID: ${session.id}, Protocol: ${session.protocolName}\n")
        sb.append("# Units: voltage=mV, current=mA, power=W, temperature=Celsius, soc=%\n")

        // CSV Header
        sb.append("sample_id,session_id,timestamp_epoch_ms,timestamp_iso,elapsed_seconds,voltage_mv,current_ma,power_watts,temperature_celsius,battery_level_percent,thermal_status\n")

        val startMs = session.startTimestamp
        for (sample in samples) {
            val elapsedSec = (sample.timestamp - startMs) / 1000
            val iso = isoFormat.format(Date(sample.timestamp))
            sb.append(sample.sampleId).append(",")
            sb.append(sample.sessionId).append(",")
            sb.append(sample.timestamp).append(",")
            sb.append(escapeCsv(iso)).append(",")
            sb.append(elapsedSec).append(",")
            sb.append(sample.voltageMv).append(",")
            sb.append(sample.currentMa).append(",")
            sb.append(String.format(Locale.US, "%.3f", sample.powerWatts)).append(",")
            sb.append(String.format(Locale.US, "%.1f", sample.temperatureCelsius)).append(",")
            sb.append(sample.batteryLevel).append(",")
            sb.append(sample.thermalStatus).append("\n")
        }

        return sb.toString()
    }

    /**
     * Exports list of all charging sessions into RFC 4180 CSV.
     */
    fun exportSessionsCsv(
        sessions: List<ChargingSessionEntity>,
        accessoriesMap: Map<Long, AccessoryProfileEntity> = emptyMap()
    ): String {
        val isoFormat = getUtcIsoFormat()
        val sb = StringBuilder()

        sb.append("# Charge Science Charging Sessions Export\n")
        sb.append("# Device: Google Pixel 8a (akita), Nominal Capacity: 4492 mAh\n")
        sb.append("# Units: power=W, current=mA, temperature=Celsius, energy=Wh, confidence=%\n")

        sb.append("session_id,start_timestamp_epoch_ms,start_timestamp_iso,end_timestamp_epoch_ms,end_timestamp_iso,duration_seconds,start_soc,end_soc,soc_gain,charge_source,protocol_name,average_power_watts,max_power_watts,average_current_ma,max_current_ma,start_temp_c,max_temp_c,end_temp_c,net_energy_wh,valid_samples,missing_samples,confidence_score,was_interrupted,accessory_nickname\n")

        for (s in sessions) {
            val startIso = isoFormat.format(Date(s.startTimestamp))
            val endIso = s.endTimestamp?.let { isoFormat.format(Date(it)) } ?: ""
            val endEpoch = s.endTimestamp ?: 0L
            val endSoc = s.endSoc ?: s.startSoc
            val socGain = endSoc - s.startSoc
            val energyWh = s.averagePowerWatts * (s.totalDurationSeconds / 3600f)
            val accessory = s.accessoryId?.let { accessoriesMap[it]?.nickname } ?: "Unassigned"

            sb.append(s.id).append(",")
            sb.append(s.startTimestamp).append(",")
            sb.append(escapeCsv(startIso)).append(",")
            sb.append(endEpoch).append(",")
            sb.append(escapeCsv(endIso)).append(",")
            sb.append(s.totalDurationSeconds).append(",")
            sb.append(s.startSoc).append(",")
            sb.append(endSoc).append(",")
            sb.append(socGain).append(",")
            sb.append(s.chargeSource).append(",")
            sb.append(escapeCsv(s.protocolName)).append(",")
            sb.append(String.format(Locale.US, "%.3f", s.averagePowerWatts)).append(",")
            sb.append(String.format(Locale.US, "%.3f", s.maxPowerWatts)).append(",")
            sb.append(s.averageCurrentMa).append(",")
            sb.append(s.maxCurrentMa).append(",")
            sb.append(String.format(Locale.US, "%.1f", s.startTempCelsius)).append(",")
            sb.append(String.format(Locale.US, "%.1f", s.maxTempCelsius)).append(",")
            sb.append(String.format(Locale.US, "%.1f", s.endTempCelsius)).append(",")
            sb.append(String.format(Locale.US, "%.3f", energyWh)).append(",")
            sb.append(s.validSampleCount).append(",")
            sb.append(s.missingSampleCount).append(",")
            sb.append(String.format(Locale.US, "%.1f", s.confidenceScore)).append(",")
            sb.append(if (s.wasInterrupted) "1" else "0").append(",")
            sb.append(escapeCsv(accessory)).append("\n")
        }

        return sb.toString()
    }

    /**
     * Exports full dataset (sessions + samples + accessories) into clean self-contained JSON.
     */
    fun exportCompleteJson(
        sessionsWithSamples: List<Pair<ChargingSessionEntity, List<TelemetrySampleEntity>>>,
        accessories: List<AccessoryProfileEntity> = emptyList()
    ): String {
        val isoFormat = getUtcIsoFormat()
        val nowIso = isoFormat.format(Date())

        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"app_name\": \"Charge Science\",\n")
        sb.append("  \"app_version\": \"").append(APP_VERSION).append("\",\n")
        sb.append("  \"schema_version\": ").append(SCHEMA_VERSION).append(",\n")
        sb.append("  \"export_timestamp_iso\": \"").append(nowIso).append("\",\n")
        sb.append("  \"device_hardware\": {\n")
        sb.append("    \"manufacturer\": \"Google\",\n")
        sb.append("    \"model\": \"Pixel 8a\",\n")
        sb.append("    \"code_name\": \"akita\",\n")
        sb.append("    \"nominal_design_capacity_mah\": ").append(NOMINAL_CAPACITY_MAH).append(",\n")
        sb.append("    \"pmic_fuel_gauge\": \"Maxim MAX77759 ModelGauge m5\"\n")
        sb.append("  },\n")

        // Accessories array
        sb.append("  \"accessories\": [\n")
        accessories.forEachIndexed { index, a ->
            sb.append("    {\n")
            sb.append("      \"id\": ").append(a.id).append(",\n")
            sb.append("      \"type\": \"").append(escapeJson(a.type)).append("\",\n")
            sb.append("      \"nickname\": \"").append(escapeJson(a.nickname)).append("\",\n")
            sb.append("      \"manufacturer\": \"").append(escapeJson(a.manufacturer)).append("\",\n")
            sb.append("      \"model\": \"").append(escapeJson(a.model)).append("\",\n")
            sb.append("      \"claimed_wattage\": ").append(a.claimedWattage).append(",\n")
            sb.append("      \"verified_protocols\": \"").append(escapeJson(a.verifiedProtocols)).append("\",\n")
            sb.append("      \"max_observed_power_watts\": ").append(a.maxObservedPowerWatts).append(",\n")
            sb.append("      \"notes\": \"").append(escapeJson(a.notes)).append("\"\n")
            sb.append("    }").append(if (index < accessories.size - 1) ",\n" else "\n")
        }
        sb.append("  ],\n")

        // Sessions array with nested samples
        sb.append("  \"sessions\": [\n")
        sessionsWithSamples.forEachIndexed { sIndex, (session, samples) ->
            val startIso = isoFormat.format(Date(session.startTimestamp))
            val endIso = session.endTimestamp?.let { isoFormat.format(Date(it)) } ?: ""
            val energyWh = session.averagePowerWatts * (session.totalDurationSeconds / 3600f)

            sb.append("    {\n")
            sb.append("      \"session_id\": ").append(session.id).append(",\n")
            sb.append("      \"start_timestamp_iso\": \"").append(startIso).append("\",\n")
            sb.append("      \"end_timestamp_iso\": \"").append(endIso).append("\",\n")
            sb.append("      \"duration_seconds\": ").append(session.totalDurationSeconds).append(",\n")
            sb.append("      \"start_soc\": ").append(session.startSoc).append(",\n")
            sb.append("      \"end_soc\": ").append(session.endSoc ?: session.startSoc).append(",\n")
            sb.append("      \"charge_source\": ").append(session.chargeSource).append(",\n")
            sb.append("      \"protocol_name\": \"").append(escapeJson(session.protocolName)).append("\",\n")
            sb.append("      \"average_power_watts\": ").append(String.format(Locale.US, "%.3f", session.averagePowerWatts)).append(",\n")
            sb.append("      \"max_power_watts\": ").append(String.format(Locale.US, "%.3f", session.maxPowerWatts)).append(",\n")
            sb.append("      \"average_current_ma\": ").append(session.averageCurrentMa).append(",\n")
            sb.append("      \"max_current_ma\": ").append(session.maxCurrentMa).append(",\n")
            sb.append("      \"start_temp_celsius\": ").append(session.startTempCelsius).append(",\n")
            sb.append("      \"max_temp_celsius\": ").append(session.maxTempCelsius).append(",\n")
            sb.append("      \"end_temp_celsius\": ").append(session.endTempCelsius).append(",\n")
            sb.append("      \"net_energy_wh\": ").append(String.format(Locale.US, "%.3f", energyWh)).append(",\n")
            sb.append("      \"valid_sample_count\": ").append(session.validSampleCount).append(",\n")
            sb.append("      \"confidence_score\": ").append(String.format(Locale.US, "%.1f", session.confidenceScore)).append(",\n")
            sb.append("      \"was_interrupted\": ").append(session.wasInterrupted).append(",\n")
            sb.append("      \"accessory_id\": ").append(session.accessoryId ?: "null").append(",\n")

            // Nested samples
            sb.append("      \"samples\": [\n")
            samples.forEachIndexed { sampleIndex, sample ->
                sb.append("        {")
                sb.append("\"id\":").append(sample.sampleId).append(",")
                sb.append("\"t\":").append(sample.timestamp).append(",")
                sb.append("\"v\":").append(sample.voltageMv).append(",")
                sb.append("\"i\":").append(sample.currentMa).append(",")
                sb.append("\"p\":").append(String.format(Locale.US, "%.3f", sample.powerWatts)).append(",")
                sb.append("\"temp\":").append(String.format(Locale.US, "%.1f", sample.temperatureCelsius)).append(",")
                sb.append("\"soc\":").append(sample.batteryLevel).append(",")
                sb.append("\"th\":").append(sample.thermalStatus)
                sb.append("}").append(if (sampleIndex < samples.size - 1) ",\n" else "\n")
            }
            sb.append("      ]\n")
            sb.append("    }").append(if (sIndex < sessionsWithSamples.size - 1) ",\n" else "\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Writes exported content to application cache directory and returns the File.
     */
    fun writeExportFile(context: Context, filename: String, content: String): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        val file = File(exportDir, filename)
        file.writeText(content, Charsets.UTF_8)
        return file
    }

    /**
     * Builds a native Android Intent.ACTION_SEND for sharing the file via system share sheet.
     */
    fun createShareIntent(context: Context, file: File, mimeType: String): Intent {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)

        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
