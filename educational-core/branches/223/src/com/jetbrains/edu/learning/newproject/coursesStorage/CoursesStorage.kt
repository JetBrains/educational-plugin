package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*

@State(
  name = "com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage",
  storages = [Storage("coursesStorage.xml", roamingType = RoamingType.DISABLED)]
)
@Service
class CoursesStorage : CoursesStorageBase() {
  fun removeCourseByLocation(location: String) {
    super.doRemoveCourseByLocation(location)
  }

  companion object {
    fun getInstance(): CoursesStorage = service()
  }
}