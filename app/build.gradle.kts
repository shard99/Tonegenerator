import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.shard9.tonegenerator"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.shard9.tonegenerator"
        minSdk = 26
        targetSdk = 36
        versionCode = 19
        versionName = "1.2.5"

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

dependencies {
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
        implementation("io.netty:netty-codec-http2:4.1.132.Final")
        implementation("io.netty:netty-codec-http:4.1.132.Final")
        implementation("org.bitbucket.b_c:jose4j:0.9.6")
        implementation("org.jdom:jdom2:2.0.6.1")
        implementation("io.netty:netty-handler:4.1.118.Final")
    }
}
