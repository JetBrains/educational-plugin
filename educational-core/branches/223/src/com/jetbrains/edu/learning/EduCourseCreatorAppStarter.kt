package com.jetbrains.edu.learning

// BACKCOMPAT: 2022.2. Merge with `EduCourseCreatorAppStarterBase`
class EduCourseCreatorAppStarter : EduCourseCreatorAppStarterBase() {
  @Suppress("OVERRIDE_DEPRECATION")
  override val commandName: String
    get() = "createCourse"
}
