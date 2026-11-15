import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ktlint)
}

android {
  namespace = "dev.shard9.tonegenerator"
  compileSdk = 37

  defaultConfig {
    applicationId = "dev.shard9.tonegenerator"
    minSdk = 26
    targetSdk = 36
    versionCode = 41
    versionName = "2.1.8"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    val buildDate = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date())
    buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions {
    unitTests {
      // AGP 9.5 registers generate<Variant>ComposePreviewRunfiles eagerly and requires
      // compiled Android resources for host tests; without this, task listing fails.
      isIncludeAndroidResources = true
    }
  }
}

ktlint {
  android = true
  ignoreFailures = false
  reporters {
    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
  }
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.material.icons.extended)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.datastore.preferences.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Dependency constraints to address high-severity security alerts on transitive dependencies.
  // Versions are defined in gradle/libs.versions.toml.
  constraints {
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)
    implementation(libs.netty.codec.http2)
    implementation(libs.netty.codec.http)
    implementation(libs.netty.handler)
    implementation(libs.netty.codec)
    implementation(libs.netty.common)
    implementation(libs.netty.handler.proxy)
    implementation(libs.jose4j)
    implementation(libs.jdom2)
    implementation(libs.protobuf.java)
    implementation(libs.commons.io)
    implementation(libs.commons.compress)
    implementation(libs.httpclient)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.guava)
    implementation(libs.json)
    implementation(libs.logback.core)
    implementation(libs.commons.lang3)
  }
}
