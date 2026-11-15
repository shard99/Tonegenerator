buildscript {
  repositories {
    google()
    mavenCentral()
  }
  dependencies {
    constraints {
      classpath("org.bouncycastle:bcprov-jdk18on:1.87")
      classpath("org.bouncycastle:bcpkix-jdk18on:1.87")
      classpath("io.netty:netty-codec-http2:4.2.20.Final")
      classpath("io.netty:netty-codec-http:4.2.20.Final")
      classpath("io.netty:netty-handler:4.2.20.Final")
      classpath("io.netty:netty-common:4.2.20.Final")
      classpath("io.netty:netty-codec:4.2.20.Final")
      classpath("io.netty:netty-handler-proxy:4.2.20.Final")
      classpath("org.bitbucket.b_c:jose4j:0.9.7")
      classpath("org.jdom:jdom2:2.0.6.1")
      classpath("ch.qos.logback:logback-core:1.6.7")
      classpath("org.apache.commons:commons-lang3:3.22.0")
      // GHSA-xxph-c9ww-hj94: AGP pulls guava 33.4.0-jre onto the plugin classpath.
      classpath("com.google.guava:guava:33.8.0-jre")
    }
  }
}

// Top-level build file where you can add configuration options common to all subprojects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.ktlint)
}

// The ktlint Gradle plugin resolves its own `ktlint*` configurations, which do not inherit the
// module dependency constraints. ktlint-cli 1.5.0 therefore brings logback 1.3.14, which is hit by
// four advisories (GHSA-6v67-2wr5-gvf4, GHSA-qqpg-mvqg-649v, GHSA-p47f-322f-whfh, GHSA-jhq6-gfmj-v8fx).
// Forcing the patched versions here keeps the lint tooling off the vulnerable releases.
configurations.matching { it.name.startsWith("ktlint") }.configureEach {
  resolutionStrategy {
    force(
      "ch.qos.logback:logback-classic:${libs.versions.logback.get()}",
      "ch.qos.logback:logback-core:${libs.versions.logback.get()}",
    )
  }
}
