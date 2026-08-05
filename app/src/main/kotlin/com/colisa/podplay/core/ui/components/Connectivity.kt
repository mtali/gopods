package com.colisa.podplay.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.colisa.podplay.R

@Composable
fun AppOffline(
  modifier: Modifier = Modifier,
  onRetry: (() -> Unit)? = null,
) {
  AppMessage(
    icon = Icons.Outlined.WifiOff,
    title = stringResource(R.string.offline_title),
    message = stringResource(R.string.offline_message),
    actionLabel = onRetry?.let { stringResource(R.string.action_retry) },
    onAction = onRetry,
    modifier = modifier,
  )
}
