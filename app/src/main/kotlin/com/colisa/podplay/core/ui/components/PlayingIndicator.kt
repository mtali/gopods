package com.colisa.podplay.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BarWidth = 3.dp

/** Peak height as a fraction of the whole, and the period, per bar. */
private val Bars = listOf(0.45f to 620, 1.0f to 480, 0.65f to 720)

/**
 * Equalizer bars that only move while playing, so a glance at a list says something is
 * running rather than merely selected.
 */
@Composable
fun PlayingIndicator(
  playing: Boolean,
  modifier: Modifier = Modifier,
  size: Dp = 14.dp,
  color: Color = MaterialTheme.colorScheme.primary,
) {
  val transition = rememberInfiniteTransition(label = "playingIndicator")

  Row(
    modifier = modifier.size(size),
    verticalAlignment = Alignment.Bottom,
    horizontalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Bars.forEach { (peak, durationMs) ->
      val animated by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = peak,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = durationMs, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse,
        ),
        label = "bar",
      )
      // Paused keeps the shape but holds still.
      val fraction = if (playing) animated else peak * 0.5f
      Box(
        modifier = Modifier
          .width(BarWidth)
          .fillMaxHeight(fraction)
          .clip(MaterialTheme.shapes.extraSmall)
          .background(color),
      )
    }
  }
}
