import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  alias(libs.plugins.androidx.room)
}

// Release signing comes from a git ignored keystore/keystore.properties. If the file is
// absent, on a fresh clone or on CI, the release build falls back to the debug key so it
// still assembles.
val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties().apply {
  if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { load(it) }
  }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

android {
  namespace = "com.colisa.podplay"
  compileSdk {
    version = release(37)
  }

  defaultConfig {
    applicationId = "com.colisa.podplay"
    minSdk = 24
    targetSdk = 36
    versionCode = 6
    versionName = "2.0.2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      if (hasReleaseKeystore) {
        storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
        storePassword = keystoreProperties.getProperty("storePassword")
        keyAlias = keystoreProperties.getProperty("keyAlias")
        keyPassword = keystoreProperties.getProperty("keyPassword")
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
      signingConfig = if (hasReleaseKeystore) {
        signingConfigs.getByName("release")
      } else {
        signingConfigs.getByName("debug")
      }
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    buildConfig = true
    compose = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  hilt {
    enableAggregatingTask = true
  }

  bundle {
    language {
      enableSplit = false
    }
  }
}

// Export a json schema per database version.
room {
  schemaDirectory("$projectDir/schemas")
}

dependencies {
  // Compose
  val composeBom = platform(libs.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  implementation(libs.compose.foundation)
  implementation(libs.compose.material.icons.extended)
  implementation(libs.compose.material3)
  implementation(libs.compose.material3.adaptive)
  implementation(libs.compose.ui)
  implementation(libs.compose.ui.graphics)
  implementation(libs.compose.ui.tooling.preview)
  debugImplementation(libs.compose.ui.tooling)
  debugImplementation(libs.compose.ui.test.manifest)

  // AndroidX
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)

  // Navigation 3
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.navigation3.ui)

  // Dependency injection
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.hilt.work)
  implementation(libs.hilt.android)
  ksp(libs.androidx.hilt.compiler)
  ksp(libs.hilt.compiler)

  // Persistence
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  // Background work
  implementation(libs.androidx.work.runtime.ktx)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // Playback
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)

  // Networking
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.serialization.converter)
  implementation(libs.rssparser)

  // Images
  implementation(libs.coil.compose)
  implementation(libs.coil.network.okhttp)

  // Misc
  implementation(libs.play.app.update.ktx)
  implementation(libs.timber)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.compose.ui.test.junit4)
}
