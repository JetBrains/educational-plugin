import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("common-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

java {
  toolchain {
    // If you change language version here, remember to change it for Kotlin below
    languageVersion = JavaLanguageVersion.of(25)
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      // If you change target version here, remember to change it for Java above
      jvmTarget = JvmTarget.JVM_25
    }
  }

  test {
    useJUnitPlatform()
  }
}

dependencies {
  compileOnly(libs.kotlinx.serialization) // compileOnly here helps not to bring the library to the plugin archive

  testImplementation(libs.kotlinx.coroutine.test)
  testImplementation(libs.kotlinx.serialization)
  testImplementation(libs.junit.jupiter)
  testRuntimeOnly(libs.junit.platform.launcher)
}
