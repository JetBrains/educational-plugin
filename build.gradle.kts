import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

val pluginVersion: String by project

idea {
  project {
    jdkName = "17"
    languageLevel = IdeaLanguageLevel("11")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file("dependencies"))
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
