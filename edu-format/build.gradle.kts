plugins {
  `maven-publish`
}

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

java {
  withSourcesJar()
}

dependencies {
  compileOnly(libs.kotlin.stdlib)
  compileOnly(libs.annotations)
  implementationWithoutKotlin(libs.jackson.module.kotlin)
  implementationWithoutKotlin(libs.jackson.dataformat.yaml)
  implementationWithoutKotlin(libs.jackson.datatype.jsr310)
  implementationWithoutKotlin(libs.retrofit)
  implementationWithoutKotlin(libs.converter.jackson)
  implementationWithoutKotlin(libs.logging.interceptor)
  implementationWithoutKotlin(libs.edu.ai.format)
}

// Workaround to help java to find `module-info.java` file.
// Is there a better way?
val moduleName = "com.jetbrains.edu.format"
tasks {
  compileJava {
    inputs.property("moduleName", moduleName)
    options.compilerArgs = listOf("--patch-module", "$moduleName=${sourceSets.main.get().output.asPath}")
  }
}

val eduFormatArtifactPathProperty = "eduFormatArtifactPath"
val eduFormatSourcesArtifactPathProperty = "eduFormatSourcesArtifactPath"

publishing {
  publications {
    create<MavenPublication>("edu-format") {
      groupId = "com.jetbrains.edu"
      artifactId = "edu-format"
      version = prop("publishingVersion")

      // Usually, we want to pass paths to already built edu-format artifacts from pinned build on TeamCity during release
      // instead of building them again.
      // It's supposed such paths are passed via `eduFormatArtifactPath` and `eduFormatSourcesArtifactPath` properties
      if (hasProperty(eduFormatArtifactPathProperty) && hasProperty(eduFormatSourcesArtifactPathProperty)) {
        artifact(prop(eduFormatArtifactPathProperty))
        artifact(prop(eduFormatSourcesArtifactPathProperty)) {
          classifier = "sources"
        }
      }
      // If `eduFormatArtifactPath` and `eduFormatSourcesArtifactPath` properties are not passed
      // build library and publish artifacts of the corresponding tasks
      else {
        // Manual artifact specification leads to absense of transitive dependencies in `pom` file
        artifact(tasks["jar"])
        artifact(tasks["sourcesJar"])
      }
    }
  }
  repositories {
    maven {
      url = uri("https://packages.jetbrains.team/maven/p/edu/maven")
      credentials {
        username = prop("publishingUsername")
        password = prop("publishingPassword")
      }
    }
  }
}

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: Provider<*>) {
  implementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
}

fun prop(name: String): String = providers.gradleProperty(name).get()
