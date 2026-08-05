import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Lists all plugins used throughout the project without applying them.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.androidx.navigation.safeargs) apply false
  alias(libs.plugins.androidx.room) apply false
  alias(libs.plugins.hilt) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.ksp) apply false
}

subprojects {
  tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }
}
