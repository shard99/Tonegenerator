buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    constraints {
      classpath("org.bouncycastle:bcprov-jdk18on:1.84")
      classpath("org.bouncycastle:bcpkix-jdk18on:1.84")
      classpath("io.netty:netty-codec-http2:4.2.15.Final")
      classpath("io.netty:netty-codec-http:4.2.15.Final")
      classpath("io.netty:netty-handler:4.2.15.Final")
      classpath("io.netty:netty-common:4.2.15.Final")
      classpath("io.netty:netty-codec:4.2.15.Final")
      classpath("io.netty:netty-handler-proxy:4.2.15.Final")
      classpath("org.bitbucket.b_c:jose4j:0.9.6")
      classpath("org.jdom:jdom2:2.0.6.1")
      classpath("ch.qos.logback:logback-core:1.5.35")
      classpath("org.apache.commons:commons-lang3:3.17.0")
    }
  }
}

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.ktlint)
}
