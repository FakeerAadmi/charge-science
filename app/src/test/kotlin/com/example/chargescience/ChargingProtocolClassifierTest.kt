package com.example.chargescience

import com.example.chargescience.domain.protocol.ChargingProtocolClassifier
import com.example.chargescience.domain.protocol.PpsVerificationStatus
import com.example.chargescience.domain.protocol.ProtocolCategory
import org.junit.Assert.*
import org.junit.Test

class ChargingProtocolClassifierTest {

    @Test
    fun testUsbPd9VContractClassification() {
        // Pixel 8a typical USB-PD: 9V @ 2.2A -> 19.8W
        val info = ChargingProtocolClassifier.classify(
            chargeSource = 1, // AC
            maxVoltageUv = 9_000_000,
            maxCurrentUa = 2_200_000
        )

        assertEquals(ProtocolCategory.USB_PD, info.protocolCategory)
        assertEquals(9000, info.negotiatedVoltageMv)
        assertEquals(2200, info.negotiatedCurrentMa)
        assertEquals(19.8f, info.negotiatedCeilingWatts, 0.01f)
        assertTrue("Must be identified as a PPS candidate", info.isPpsCandidate)
        // Strictly must NOT be declared as confirmed PPS without external hardware meter!
        assertEquals(PpsVerificationStatus.CANDIDATE_UNVERIFIED, info.ppsVerificationStatus)
        assertTrue(info.protocolName.contains("USB Power Delivery"))
    }

    @Test
    fun testQiWirelessClassification() {
        // Pixel 8a Qi EPP 7.5W
        val eppInfo = ChargingProtocolClassifier.classify(
            chargeSource = 4, // Wireless
            maxVoltageUv = 9_000_000,
            maxCurrentUa = 833_000
        )
        assertEquals(ProtocolCategory.QI_WIRELESS, eppInfo.protocolCategory)
        assertTrue("Should detect Qi Extended Power Profile", eppInfo.protocolName.contains("Extended Power Profile"))
        assertFalse("Wireless cannot be PPS candidate", eppInfo.isPpsCandidate)

        // Qi BPP 5W fallback
        val bppInfo = ChargingProtocolClassifier.classify(
            chargeSource = 4,
            maxVoltageUv = 5_000_000,
            maxCurrentUa = 1_000_000
        )
        assertEquals(ProtocolCategory.QI_WIRELESS, bppInfo.protocolCategory)
        assertTrue("Should detect Qi Baseline", bppInfo.protocolName.contains("Baseline Power Profile"))
    }

    @Test
    fun testTypeC3AProfile() {
        val info = ChargingProtocolClassifier.classify(
            chargeSource = 1,
            maxVoltageUv = 5_000_000,
            maxCurrentUa = 3_000_000
        )
        assertEquals(ProtocolCategory.USB_TYPE_C_CURRENT, info.protocolCategory)
        assertEquals(15.0f, info.negotiatedCeilingWatts, 0.01f)
        assertTrue(info.protocolName.contains("3.0A"))
    }

    @Test
    fun testBc12DcpProfile() {
        val info = ChargingProtocolClassifier.classify(
            chargeSource = 1,
            maxVoltageUv = 5_000_000,
            maxCurrentUa = 1_500_000
        )
        assertEquals(ProtocolCategory.USB_BC_1_2, info.protocolCategory)
        assertEquals(7.5f, info.negotiatedCeilingWatts, 0.01f)
        assertTrue(info.protocolName.contains("BC 1.2"))
    }

    @Test
    fun testDischargingOrUnknownSource() {
        val info = ChargingProtocolClassifier.classify(
            chargeSource = 0,
            maxVoltageUv = 0,
            maxCurrentUa = 0
        )
        assertEquals(ProtocolCategory.DISCHARGING, info.protocolCategory)
        assertEquals(0f, info.negotiatedCeilingWatts, 0.0f)
    }
}
