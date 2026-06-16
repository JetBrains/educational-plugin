import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("common-conventions")
  alias(libs.plugins.kotlinSerializationPlugin)
}

java {
  toolchain {
    // If you change language version here, remember to change it for Kotlin below
    languageVersion = JavaLanguageVersion.of(21)
  }
}

tasks {
  withType<KotlinCompile> {
    compilerOptions {
      // If you change target version here, remember to change it for Java above
      jvmTarget = JvmTarget.JVM_21
    }
  }
}

dependencies {
  compileOnly(libs.kotlin.stdlib)
  compileOnly(libs.annotations)
  implementation(libs.kotlinx.serialization)
}
