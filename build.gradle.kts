import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

// These repositories already exist in the educational-core module, but we need them here to make
// IDE see platform sources during development. This is a workaround and should be removed after
// the intelliJ Platform Gradle Plugin fixes source download.
repositories {
  maven("https://www.jetbrains.com/intellij-repository/releases")
  maven("https://www.jetbrains.com/intellij-repository/snapshots")
}

idea {
  project {
    jdkName = "21"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
    excludeDirs.add(file(".intellijPlatform"))
  }
}

// For some reason, `version = "$pluginVersion.0"` inside `fleet-plugin/build.gradle.kts` is not enough.
// It seems fleet gradle plugin reads project version too early when it's not set yet.
// This code executed before `fleet-plugin/build.gradle.kts` is evaluated,
// so at the moment of reading version is already set.
//
// `.0` is needed because fleet plugin should have only `major.minor.patch` version structure
if (prop("fleetIntegration").toBoolean()) {
  project(":fleet-plugin") {
    version = "$pluginVersion.0"
  }
}

tasks {
  wrapper {
    distributionType = Wrapper.DistributionType.ALL
  }
}
