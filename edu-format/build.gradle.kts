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
