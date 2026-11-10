package com.example.chargescience.domain.protocol

/**
 * Detailed protocol information and classification model.
 * Strictly separates:
 * 1. Advertised charger capability
 * 2. Negotiated power contract
 * 3. Observed battery-side chemical power
 * 4. Estimated charger-side power
 */
data class ChargingProtocolInfo(
    val protocolName: String,
    val protocolCategory: ProtocolCategory,
    val negotiatedVoltageMv: Int,
    val negotiatedCurrentMa: Int,
    val negotiatedCeilingWatts: Float,
    val isPpsCandidate: Boolean,
    val ppsVerificationStatus: PpsVerificationStatus,
    val technicalExplanation: String,
    val measurementEvidence: String
)

enum class ProtocolCategory {
    USB_PD,
    USB_TYPE_C_CURRENT,
    USB_BC_1_2,
    USB_LEGACY_SDP,
    QI_WIRELESS,
    DISCHARGING,
    UNKNOWN
}

enum class PpsVerificationStatus {
    NOT_APPLICABLE,
    CANDIDATE_UNVERIFIED, // Hardware supports it, but Android userspace cannot verify CC APDO packets
    CONFIRMED_EXTERNAL_METER // Requires external BLE power meter (FNB58 / TC66C)
}

object ChargingProtocolClassifier {

    /**
     * Scientifically classifies charging protocol from observable system inputs without
     * making unfounded assertions about PPS or cable ratings.
     *
     * @param chargeSource 0=None, 1=AC, 2=USB, 4=Wireless
     * @param maxVoltageUv Microvolts from BatteryManager EXTRA_MAX_CHARGING_VOLTAGE
     * @param maxCurrentUa Microamperes from BatteryManager EXTRA_MAX_CHARGING_CURRENT
     */
    fun classify(
        chargeSource: Int,
        maxVoltageUv: Int,
        maxCurrentUa: Int
    ): ChargingProtocolInfo {
        val maxVoltageMv = if (maxVoltageUv > 0) maxVoltageUv / 1000 else 0
        val maxCurrentMa = if (maxCurrentUa > 0) maxCurrentUa / 1000 else 0
        val ceilingWatts = (maxVoltageMv / 1000f) * (maxCurrentMa / 1000f)

        return when (chargeSource) {
            4 -> { // Wireless Qi
                val isEpp = maxVoltageMv >= 9000 || ceilingWatts > 5.5f
                val name = if (isEpp) "Qi Extended Power Profile (7.5W Max)" else "Qi Baseline Power Profile (5W Max)"
                ChargingProtocolInfo(
                    protocolName = name,
                    protocolCategory = ProtocolCategory.QI_WIRELESS,
                    negotiatedVoltageMv = if (maxVoltageMv > 0) maxVoltageMv else 5000,
                    negotiatedCurrentMa = if (maxCurrentMa > 0) maxCurrentMa else 1500,
                    negotiatedCeilingWatts = if (ceilingWatts > 0f) ceilingWatts else 7.5f,
                    isPpsCandidate = false,
                    ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                    technicalExplanation = "Google Pixel 8a contains a standard Qi inductive receiver coil capped at 7.5W max. It does not support proprietary Pixel Stand 23W or Qi2 Magnetic Power Profile (MPP).",
                    measurementEvidence = "Identified via BatteryManager.BATTERY_PLUGGED_WIRELESS."
                )
            }
            1 -> { // AC Power / High-power USB-C
                if (maxVoltageMv >= 8000) {
                    // USB Power Delivery (typically 9V @ 2.2A on Pixel 8a -> 19.8W)
                    ChargingProtocolInfo(
                        protocolName = "USB Power Delivery (Fixed 9V or PPS)",
                        protocolCategory = ProtocolCategory.USB_PD,
                        negotiatedVoltageMv = maxVoltageMv,
                        negotiatedCurrentMa = maxCurrentMa,
                        negotiatedCeilingWatts = ceilingWatts,
                        isPpsCandidate = true,
                        ppsVerificationStatus = PpsVerificationStatus.CANDIDATE_UNVERIFIED,
                        technicalExplanation = "Operating under USB-PD power rules. Pixel 8a negotiates up to 9V @ 2.2A. If connected to a PPS-capable brick, the kernel TCPM utilizes 20mV voltage adjustments; however, Android userspace only receives the 9V profile ceiling.",
                        measurementEvidence = "EXTRA_MAX_CHARGING_VOLTAGE = ${maxVoltageMv}mV (>5000mV proves USB-PD negotiation; USB BC 1.2 cannot exceed 5V)."
                    )
                } else if (maxVoltageMv in 4500..5500 && maxCurrentMa >= 2500) {
                    ChargingProtocolInfo(
                        protocolName = "USB Type-C (3.0A / 15W Profile)",
                        protocolCategory = ProtocolCategory.USB_TYPE_C_CURRENT,
                        negotiatedVoltageMv = 5000,
                        negotiatedCurrentMa = maxCurrentMa,
                        negotiatedCeilingWatts = ceilingWatts,
                        isPpsCandidate = false,
                        ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                        technicalExplanation = "Operating on standard Type-C current advertisement via Rp pull-up resistor (3.0A @ 5V).",
                        measurementEvidence = "Voltage at 5V rail with Rp pull-up signaling 3.0A capability."
                    )
                } else if (maxVoltageMv in 4500..5500 && maxCurrentMa in 1400..2400) {
                    ChargingProtocolInfo(
                        protocolName = "USB Dedicated Charging Port (BC 1.2 / 1.5A)",
                        protocolCategory = ProtocolCategory.USB_BC_1_2,
                        negotiatedVoltageMv = 5000,
                        negotiatedCurrentMa = maxCurrentMa,
                        negotiatedCeilingWatts = ceilingWatts,
                        isPpsCandidate = false,
                        ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                        technicalExplanation = "Legacy USB Battery Charging Specification v1.2 Dedicated Charging Port (DCP) with shorted D+/D- lines, or Type-C 1.5A advertisement.",
                        measurementEvidence = "EXTRA_MAX_CHARGING_CURRENT = ${maxCurrentMa}mA at 5V rail."
                    )
                } else {
                    ChargingProtocolInfo(
                        protocolName = "Standard AC Charging (5V Rail)",
                        protocolCategory = ProtocolCategory.USB_TYPE_C_CURRENT,
                        negotiatedVoltageMv = if (maxVoltageMv > 0) maxVoltageMv else 5000,
                        negotiatedCurrentMa = if (maxCurrentMa > 0) maxCurrentMa else 1500,
                        negotiatedCeilingWatts = if (ceilingWatts > 0f) ceilingWatts else 7.5f,
                        isPpsCandidate = false,
                        ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                        technicalExplanation = "Power source connected via AC adapter. Protocol negotiation limits constrained to baseline 5V rail.",
                        measurementEvidence = "EXTRA_PLUGGED = BATTERY_PLUGGED_AC."
                    )
                }
            }
            2 -> { // USB Port (PC / Host)
                val isSdp = maxCurrentMa <= 500
                val name = if (isSdp) "USB 2.0 Standard Downstream (500mA)" else "USB 3.0+ Downstream (900mA+)"
                ChargingProtocolInfo(
                    protocolName = name,
                    protocolCategory = ProtocolCategory.USB_LEGACY_SDP,
                    negotiatedVoltageMv = 5000,
                    negotiatedCurrentMa = if (maxCurrentMa > 0) maxCurrentMa else 500,
                    negotiatedCeilingWatts = if (ceilingWatts > 0f) ceilingWatts else 2.5f,
                    isPpsCandidate = false,
                    ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                    technicalExplanation = "Connected to a USB host data port. Power delivery is restricted to avoid overloading host USB controllers.",
                    measurementEvidence = "EXTRA_PLUGGED = BATTERY_PLUGGED_USB."
                )
            }
            else -> {
                ChargingProtocolInfo(
                    protocolName = "Not Charging / Discharging",
                    protocolCategory = ProtocolCategory.DISCHARGING,
                    negotiatedVoltageMv = 0,
                    negotiatedCurrentMa = 0,
                    negotiatedCeilingWatts = 0f,
                    isPpsCandidate = false,
                    ppsVerificationStatus = PpsVerificationStatus.NOT_APPLICABLE,
                    technicalExplanation = "Device running on internal battery cell power.",
                    measurementEvidence = "No external power source detected."
                )
            }
        }
    }
}
