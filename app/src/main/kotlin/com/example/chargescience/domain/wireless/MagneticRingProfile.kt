package com.example.chargescience.domain.wireless

/**
 * Geometric and magnetic parameters of an aftermarket ring attached to the phone.
 *
 * Pixel 8a physical constants:
 * Height: 152.1 mm, Width: 72.7 mm, Thickness: 8.9 mm
 * Internal Qi Rx coil center: X = 36.35 mm (centered), Y ≈ 71 mm from bottom (near volume down button)
 * Rear "G" logo center: X = 36.35 mm, Y ≈ 78 mm from bottom (~7 mm higher than coil center)
 */
data class MagneticRingProfile(
    val outerDiameterMm: Float = 55.0f,
    val innerDiameterMm: Float = 44.0f,
    val thicknessMm: Float = 1.0f,
    val offsetFromCoilCenterMm: Float = 0.0f,
    val isPermanentMagnet: Boolean = true, // true = Neodymium (NdFeB) array, false = passive ferritic steel
    val brandOrModel: String = ""
) {
    /**
     * Checks if the ring dimensions or offset place conductive material into the active flux aperture.
     * The internal Qi Rx coil has an active outer diameter of approximately 42 mm.
     * If inner diameter < 44 mm or offset > 3 mm, metallic intersection occurs.
     */
    val hasApertureOverlapRisk: Boolean
        get() = innerDiameterMm < 43.0f || offsetFromCoilCenterMm > 3.0f

    /**
     * Estimates relative eddy-current dissipation risk.
     * Solid ferromagnetic rings (unsegmented steel) experience severe continuous eddy loops.
     * Segmented NdFeB arrays experience lower loop currents but strong static bias fields.
     */
    val eddyCurrentRiskTier: EddyRiskTier
        get() = when {
            hasApertureOverlapRisk && !isPermanentMagnet -> EddyRiskTier.CRITICAL_UNSEGMENTED_METAL_IN_FLUX
            hasApertureOverlapRisk && isPermanentMagnet -> EddyRiskTier.HIGH_MAGNETIC_ARRAY_OVERLAP
            !hasApertureOverlapRisk && !isPermanentMagnet -> EddyRiskTier.MODERATE_PERIPHERAL_EDDY
            else -> EddyRiskTier.LOW_ALIGNED_SEGMENTED
        }
}

enum class EddyRiskTier(val label: String, val description: String) {
    LOW_ALIGNED_SEGMENTED("Low", "Ring sits clear of coil aperture with segmented poles."),
    MODERATE_PERIPHERAL_EDDY("Moderate", "Passive metal ring near peripheral flux lines."),
    HIGH_MAGNETIC_ARRAY_OVERLAP("High", "Magnetic array intersects active induction flux zone."),
    CRITICAL_UNSEGMENTED_METAL_IN_FLUX("Critical", "Continuous conductive metal sits directly in high-frequency alternating magnetic field.")
}
