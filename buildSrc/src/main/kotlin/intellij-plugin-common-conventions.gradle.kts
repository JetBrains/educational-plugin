import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("common-conventions")
  id("net.saliman.properties")
}

java {
  sourceSets {
    main {
      java.srcDirs("branches/$environmentName/src")
      resources.srcDirs("branches/$environmentName/resources")
    }

    test {
      java.srcDirs("branches/$environmentName/testSrc")
      resources.srcDirs("branches/$environmentName/testResources")
    }
  }
}

kotlin {
  sourceSets {
    main {
      kotlin.srcDirs("branches/$environmentName/src")
    }
    test {
      kotlin.srcDirs("branches/$environmentName/testSrc")
    }
  }
}

configurations {
  val configurations = listOf(api, implementation, compileOnly, testApi, testImplementation)
  for (configuration in configurations) {
    // The corresponding libs are already included into platform libs (sometimes in some specific form),
    // so adding them again may break the plugin in runtime or tests
    configuration {
      // Kotlin stdlib
      exclude(module = "kotlin-runtime")
      exclude(module = "kotlin-reflect")
      exclude(module = "kotlin-stdlib")
      exclude(module = "kotlin-stdlib-common")
      exclude(module = "kotlin-stdlib-jdk8")
      exclude(module = "kotlin-stdlib-jdk7")
      // Kotlin coroutines
      exclude(module = "kotlinx-coroutines-bom")
      exclude(module = "kotlinx-coroutines-core")
      exclude(module = "kotlinx-coroutines-core-jvm")
      exclude(module = "kotlinx-coroutines-jdk8")
      exclude(module = "kotlinx-coroutines-slf4j")
    }
  }
}

// It's not possible to use version catalogs in convenrion plugin as usual,
// so we have to get the catalog itself and libraries manually
// See https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
val libs = the<VersionCatalogsExtension>().named("libs")

// TODO: move dependencies into particular module `build.gradle.kts`.
//  Most modules don't need all (or even any) these dependencies
dependencies {
  implementation(libs.findLibrary("jsoup").get())
  implementation(libs.findLibrary("jackson.dataformat.yaml").get())
  implementation(libs.findLibrary("jackson.datatype.jsr310").get())
  implementation(libs.findLibrary("jackson.module.kotlin").get())
  implementation(libs.findLibrary("okhttp").get())
  implementation(libs.findLibrary("logging.interceptor").get())
  implementation(libs.findLibrary("retrofit").get())
  implementation(libs.findLibrary("converter.jackson").get())
  implementation(libs.findLibrary("kotlin.css.jvm").get())

  testImplementation(libs.findLibrary("junit").get())
  testImplementation(libs.findLibrary("openTest4J").get())
  testImplementation(libs.findLibrary("classgraph").get())
  testImplementation(libs.findLibrary("kotlin.test.junit").get())
  testImplementation(libs.findLibrary("mockwebserver").get())
  testImplementation(libs.findLibrary("mockk").get())
}

tasks {
  withType<JavaCompile> {
    // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
    inputs.property("environmentName", providers.gradleProperty("environmentName"))
  }
  withType<KotlinCompile> {
    // Prevents unexpected incremental compilation errors after changing value of `environmentName` property
    inputs.property("environmentName", providers.gradleProperty("environmentName"))
  }
}
