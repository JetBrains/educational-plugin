package com.jetbrains.edu.learning.checker

import com.jetbrains.edu.learning.newproject.EmptyProjectSettings

class PlaintTextCheckerFixture : EduCheckerFixture<EmptyProjectSettings>() {
  override val projectSettings: EmptyProjectSettings get() = EmptyProjectSettings
}
