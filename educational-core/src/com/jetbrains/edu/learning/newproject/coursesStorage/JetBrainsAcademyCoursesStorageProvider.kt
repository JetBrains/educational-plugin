package com.jetbrains.edu.learning.newproject.coursesStorage

import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CoursesStorage
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CoursesStorageProvider

class JetBrainsAcademyCoursesStorageProvider : CoursesStorageProvider {
  override fun getCoursesStorage(): CoursesStorage {
    return JBCoursesStorage.getInstance()
  }
}