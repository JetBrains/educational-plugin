package com.jetbrains.edu.learning.newproject.coursesStorage


import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseDataStorage
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CoursesStorageProvider

class JetBrainsAcademyCoursesStorageProvider : CoursesStorageProvider {
  override fun getCoursesStorage(): CourseDataStorage {
    return CoursesStorage.getInstance()
  }
}