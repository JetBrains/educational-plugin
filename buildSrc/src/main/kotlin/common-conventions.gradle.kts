import org.gradle.api.JavaVersion.VERSION_17
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
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

  sourceCompatibility = VERSION_17
  targetCompatibility = VERSION_17
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

repositories {
  mavenCentral()
  maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
  maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers")
  maven("https://packages.jetbrains.team/maven/p/edu/maven")
  maven("https://packages.jetbrains.team/maven/p/edu/educational-ml-library")
  maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
}

val isTeamCity: Boolean get() = System.getenv("TEAMCITY_VERSION") != null

tasks {
  withType<JavaCompile> {
    options.encoding = "UTF-8"
  }
  withType<KotlinCompile> {
    compilerOptions {
      jvmTarget = JvmTarget.JVM_17
      languageVersion = KotlinVersion.DEFAULT
      // see https://plugins.jetbrains.com/docs/intellij/using-kotlin.html#kotlin-standard-library
      apiVersion = KotlinVersion.KOTLIN_1_8
      freeCompilerArgs = listOf("-Xjvm-default=all")
    }
  }

  withType<Test> {
    withProp("excludeTests") { exclude(it) }

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

  jar {
    // Starting from gradle-intellij-plugin 1.6.0, test runs produces `classpath.index` file in `class` directory
    // But this file shouldn't be included into final module artifact at all, so exclude it manually for now
    exclude("**/classpath.index")
  }
}
