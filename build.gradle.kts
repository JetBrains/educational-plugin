import org.gradle.plugins.ide.idea.model.IdeaLanguageLevel

plugins {
  idea
  id("common-conventions")
}

idea {
  project {
    jdkName = "25"
    languageLevel = IdeaLanguageLevel("25")
    vcs = "Git"
  }
  module {
    excludeDirs.add(file(".intellijPlatform"))
    excludeDirs.add(file("out"))
  }
}

tasks {
  wrapper {
    distributionType = Wrapper.DistributionType.ALL
  }
}
