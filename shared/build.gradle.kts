import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidLibrary)
  kotlin("plugin.serialization") version "2.1.21"
}

kotlin {
  androidTarget {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }

  jvm()

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    browser {
      val rootDirPath = project.rootDir.path
      val projectDirPath = project.projectDir.path
      commonWebpackConfig {
        devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
          static = (static ?: mutableListOf()).apply {
            // Serve sources to debug inside browser
            add(rootDirPath)
            add(projectDirPath)
          }
        }
      }
    }
  }

  sourceSets {
    commonMain.dependencies {
      // put your Multiplatform dependencies here
      implementation(libs.kotlinx.serializationCore)
      implementation(libs.kotlinx.serializationJson)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
    }
  }
}

android {
  namespace = "tenshi.hinanawi.filebrowser.shared"
  compileSdk = libs.versions.android.compileSdk.get().toInt()
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  defaultConfig {
    minSdk = libs.versions.android.minSdk.get().toInt()
  }
}
