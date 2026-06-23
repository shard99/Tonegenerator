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
    versionCode = 37
    versionName = "2.1.4"

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
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Dependency constraints to address high-severity Dependabot security alerts
  constraints {
    implementation("org.bouncycastle:bcprov-jdk18on:1.84")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.84")
    implementation("io.netty:netty-codec-http2:4.2.15.Final")
    implementation("io.netty:netty-codec-http:4.2.15.Final")
    implementation("io.netty:netty-handler:4.2.15.Final")
    implementation("io.netty:netty-codec:4.2.15.Final")
    implementation("io.netty:netty-common:4.2.15.Final")
    implementation("io.netty:netty-handler-proxy:4.2.15.Final")
    implementation("org.bitbucket.b_c:jose4j:0.9.6")
    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("com.google.protobuf:protobuf-java:4.35.0")
    implementation("commons-io:commons-io:2.22.0")
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("com.google.guava:guava:33.6.0-jre")
    implementation("org.json:json:20260522")
    implementation("ch.qos.logback:logback-core:1.5.35")
    implementation("org.apache.commons:commons-lang3:3.17.0")
  }
}
