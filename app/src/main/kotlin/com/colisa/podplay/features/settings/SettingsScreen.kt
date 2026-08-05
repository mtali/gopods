package com.colisa.podplay.features.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colisa.podplay.R
import com.colisa.podplay.core.datastore.MAX_FAST_SEEK_SECONDS
import com.colisa.podplay.core.datastore.MIN_FAST_SEEK_SECONDS
import com.colisa.podplay.core.datastore.ThemeMode
import com.colisa.podplay.core.datastore.UserPreferences
import com.colisa.podplay.core.ui.theme.supportsDynamicColor

@Composable
fun SettingsRoute(
  onBackClick: () -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  // From API 33 notifications need a runtime grant, so enabling the setting asks for
  // it first and only turns on if it was allowed.
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission(),
  ) { granted ->
    viewModel.onNotifyNewEpisodesChange(granted)
    if (!granted) {
      Toast.makeText(
        context,
        context.getString(R.string.notification_permission_rationale),
        Toast.LENGTH_LONG,
      ).show()
    }
  }

  SettingsScreen(
    preferences = uiState,
    onBackClick = onBackClick,
    onThemeModeChange = viewModel::onThemeModeChange,
    onDynamicColorChange = viewModel::onDynamicColorChange,
    onNotifyNewEpisodesChange = { enabled ->
      val needsPermission = enabled &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(
          context,
          Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED

      if (needsPermission) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      } else {
        viewModel.onNotifyNewEpisodesChange(enabled)
      }
    },
    onFastSeekSecondsChange = viewModel::onFastSeekSecondsChange,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  preferences: UserPreferences,
  onBackClick: () -> Unit,
  onThemeModeChange: (ThemeMode) -> Unit,
  onDynamicColorChange: (Boolean) -> Unit,
  onNotifyNewEpisodesChange: (Boolean) -> Unit,
  onFastSeekSecondsChange: (Int) -> Unit,
) {
  var showAbout by remember { mutableStateOf(false) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.settings)) },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = stringResource(R.string.back),
            )
          }
        },
      )
    },
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState()),
    ) {
      SectionHeader(stringResource(R.string.category_ui))

      ThemeMode.entries.forEach { mode ->
        ListItem(
          modifier = Modifier.clickable { onThemeModeChange(mode) },
          headlineContent = { Text(stringResource(mode.labelRes())) },
          leadingContent = {
            RadioButton(
              selected = preferences.themeMode == mode,
              onClick = { onThemeModeChange(mode) },
            )
          },
        )
      }

      if (supportsDynamicColor) {
        ListItem(
          modifier = Modifier.clickable {
            onDynamicColorChange(!preferences.useDynamicColor)
          },
          headlineContent = { Text(stringResource(R.string.pref_dynamic_color_title)) },
          supportingContent = { Text(stringResource(R.string.pref_dynamic_color_summary)) },
          trailingContent = {
            Switch(
              checked = preferences.useDynamicColor,
              onCheckedChange = onDynamicColorChange,
            )
          },
        )
      }

      HorizontalDivider()
      SectionHeader(stringResource(R.string.category_general))

      ListItem(
        modifier = Modifier.clickable {
          onNotifyNewEpisodesChange(!preferences.notifyNewEpisodes)
        },
        headlineContent = { Text(stringResource(R.string.pref_episode_notify_title)) },
        supportingContent = {
          Text(
            stringResource(
              if (preferences.notifyNewEpisodes) {
                R.string.pref_episode_notify_summary_on
              } else {
                R.string.pref_episode_notify_summary_off
              }
            )
          )
        },
        trailingContent = {
          Switch(
            checked = preferences.notifyNewEpisodes,
            onCheckedChange = onNotifyNewEpisodesChange,
          )
        },
      )

      FastSeekSetting(
        seconds = preferences.fastSeekSeconds,
        onChange = onFastSeekSecondsChange,
      )

      HorizontalDivider()

      ListItem(
        modifier = Modifier.clickable { showAbout = true },
        headlineContent = { Text(stringResource(R.string.about)) },
      )
    }
  }

  if (showAbout) {
    AlertDialog(
      onDismissRequest = { showAbout = false },
      title = { Text(stringResource(R.string.about)) },
      text = { Text(stringResource(R.string.about_message)) },
      confirmButton = {
        TextButton(onClick = { showAbout = false }) {
          Text(stringResource(R.string.action_cool))
        }
      },
    )
  }
}

@Composable
private fun FastSeekSetting(seconds: Int, onChange: (Int) -> Unit) {
  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    Text(
      text = stringResource(R.string.pref_fast_seeking_title),
      style = MaterialTheme.typography.bodyLarge,
    )
    Text(
      text = stringResource(R.string.pref_fast_seeking_value, seconds),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
      value = seconds.toFloat(),
      valueRange = MIN_FAST_SEEK_SECONDS.toFloat()..MAX_FAST_SEEK_SECONDS.toFloat(),
      steps = (MAX_FAST_SEEK_SECONDS - MIN_FAST_SEEK_SECONDS) / 5 - 1,
      onValueChange = { onChange(it.toInt()) },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
  )
}

private fun ThemeMode.labelRes(): Int = when (this) {
  ThemeMode.LIGHT -> R.string.pref_theme_light_title
  ThemeMode.DARK -> R.string.pref_theme_dark_title
  ThemeMode.SYSTEM -> R.string.pref_theme_auto_title
}
