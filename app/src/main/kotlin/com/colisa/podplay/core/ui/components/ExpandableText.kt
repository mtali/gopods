package com.colisa.podplay.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.colisa.podplay.R

/** Replaces the readmore-textview widget the View layouts used. */
@Composable
fun ExpandableText(
  text: String,
  modifier: Modifier = Modifier,
  collapsedMaxLines: Int = 3,
) {
  var expanded by remember(text) { mutableStateOf(false) }
  var hasOverflow by remember(text) { mutableStateOf(false) }

  Column(modifier = modifier.animateContentSize()) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = if (expanded) Int.MAX_VALUE else collapsedMaxLines,
      onTextLayout = { result -> hasOverflow = result.hasVisualOverflow || expanded },
    )
    if (hasOverflow) {
      Text(
        text = stringResource(if (expanded) R.string.read_less else R.string.read_more),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable { expanded = !expanded },
      )
    }
  }
}
