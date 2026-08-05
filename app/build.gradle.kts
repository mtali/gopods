plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  // DataBinding needs kapt to pick up the @BindingAdapter functions. Both go away
  // when the Compose UI replaces the layouts. Version comes from the Kotlin plugin
  // already on the classpath.
  id("org.jetbrains.kotlin.kapt")
  alias(libs.plugins.ksp)
  alias(libs.plugins.hilt)
  // Safe Args stays until the fragments are replaced by Compose navigation.
  alias(libs.plugins.androidx.navigation.safeargs)
  alias(libs.plugins.androidx.room)
}

android {
  namespace = "com.colisa.podplay"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.colisa.podplay"
    minSdk = 24
    // Stays on 34 until the Compose UI lands. Targeting 36 makes edge-to-edge
    // mandatory, which the current View layouts do not handle.
    targetSdk = 34
    versionCode = 3
    versionName = "1.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  room {
    schemaDirectory("$projectDir/schemas")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    buildConfig = true
    dataBinding = true
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

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.navigation.fragment.ktx)
  implementation(libs.androidx.navigation.ui.ktx)
  implementation(libs.androidx.preference.ktx)
  implementation(libs.androidx.swiperefreshlayout)
  implementation(libs.google.material)

  // Dependency injection
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Persistence
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)

  // Background work
  implementation(libs.androidx.work.runtime.ktx)

  // Coroutines
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)

  // Playback
  implementation(libs.androidx.media)
  implementation(libs.exoplayer.core)
  implementation(libs.exoplayer.mediasession)
  implementation(libs.exoplayer.ui)
  implementation(libs.support.media.compat)

  // Networking
  implementation(libs.okhttp)
  implementation(libs.okhttp.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.gson)
  implementation(libs.rssparser)

  // Serialization
  implementation(libs.moshi.kotlin)
  ksp(libs.moshi.kotlin.codegen)

  // Images
  implementation(libs.glide)

  // UI
  implementation(libs.halfbit.edge.to.edge)
  implementation(libs.material.dialogs.bottomsheets)
  implementation(libs.material.dialogs.core)
  implementation(libs.readmore.textview)

  // Misc
  implementation(libs.play.app.update.ktx)
  implementation(libs.timber)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.test.espresso.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
}
