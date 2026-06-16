import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("java")
  id("kotlin")
  id("org.gradle.test-retry")
}

java {
  sourceSets {
    main {
      java.srcDirs("src")
      resources.srcDirs("resources")
    }

    test {
      java.srcDirs("testSrc")
      resources.srcDirs("testResources")
    }
  }
}

kotlin {
  sourceSets {
    main {
      kotlin.srcDirs("src")
    }
    test {
      kotlin.srcDirs("testSrc")
    }
  }
}

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
  }
  withType<KotlinCompile> {
    compilerOptions {
      languageVersion = KotlinVersion.DEFAULT
      // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
      // BACKCOMPAT: 2026.1. Check the minimal required API version.
      // Update this message and api version value if needed
      apiVersion = KotlinVersion.KOTLIN_2_3
      freeCompilerArgs = listOf(
        "-jvm-default=no-compatibility",
        "-Xannotation-default-target=param-property"
      )
    }
  }

  withType<Test> {
    withProp("excludeTests") { exclude(it) }
    systemProperty("java.awt.headless", "true")

    ignoreFailures = true
    filter {
      isFailOnNoMatchingTests = false
    }
    if (isTeamCity) {
      retry {
        maxRetries = 3
        maxFailures = 5
      }
    }
  }
}
