package com.colisa.podplay.core.ui.theme

import androidx.compose.material3.Typography

// Material 3 defaults with a slightly tighter title weight for list rows.
private val baseline = Typography()

val AppTypography = Typography(
  titleLarge = baseline.titleLarge,
  titleMedium = baseline.titleMedium,
  bodyLarge = baseline.bodyLarge,
  bodyMedium = baseline.bodyMedium,
  labelMedium = baseline.labelMedium,
  labelSmall = baseline.labelSmall,
)
