import org.gradle.api.tasks.testing.Test
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.ktlint)
  jacoco
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

// See the comment in the root build file: the ktlint configurations do not inherit the dependency
// constraints below, so they resolve logback 1.3.14 through ktlint-cli unless it is forced here.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
  resolutionStrategy {
    force(
      "ch.qos.logback:logback-classic:${libs.versions.logback.get()}",
      "ch.qos.logback:logback-core:${libs.versions.logback.get()}",
    )
  }
}

jacoco {
  toolVersion = libs.versions.jacoco.get()
}

// Robolectric loads classes without a location, so the agent must instrument those too.
tasks.withType<Test>().configureEach {
  extensions.configure<JacocoTaskExtension> {
    isIncludeNoLocationClasses = true
    excludes = listOf("jdk.internal.*")
  }
}

// Coverage for the debug unit tests: ./gradlew jacocoDebugUnitTestReport
// HTML lands in app/build/reports/jacoco/jacocoDebugUnitTestReport/html/index.html
tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
  group = "verification"
  description = "Generates a coverage report for the debug unit tests."
  dependsOn("testDebugUnitTest")

  // AGP 9 compiles Kotlin into built_in_kotlinc instead of the historical tmp/kotlin-classes.
  val kotlinClasses =
    layout.buildDirectory.dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes")
  val javaClasses =
    layout.buildDirectory.dir("intermediates/javac/debug/compileDebugJavaWithJavac/classes")

  doFirst {
    val classesDir = kotlinClasses.get().asFile
    check(classesDir.exists()) {
      "No compiled Kotlin classes at $classesDir. If AGP moved its output, update jacocoDebugUnitTestReport."
    }
  }

  classDirectories.setFrom(
    files(kotlinClasses, javaClasses).asFileTree.matching {
      exclude(
        "**/R.class",
        "**/R\$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*ComposableSingletons*.*",
      )
    },
  )
  sourceDirectories.setFrom(files("src/main/java"))
  executionData.setFrom(layout.buildDirectory.file("jacoco/testDebugUnitTest.exec"))

  reports {
    html.required.set(true)
    xml.required.set(true)
    csv.required.set(false)
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
  // Compose UI tests run under Robolectric and need the debug manifest (ui-test-manifest), which only
  // exists for the debug variant. They therefore live in src/testDebug rather than src/test.
  testDebugImplementation(libs.robolectric)
  testDebugImplementation(platform(libs.androidx.compose.bom))
  testDebugImplementation(libs.androidx.compose.ui.test.junit4)
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
