package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.lang.Language
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse

class HyperskillCoursesProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    val supportedLanguages = EduConfiguratorManager.allExtensions()
      .filter { it.courseType == HYPERSKILL_TYPE && it.language != EduNames.JAVASCRIPT && it.environment == EduNames.DEFAULT_ENVIRONMENT }
      .mapNotNull { Language.findLanguageByID(it.language) }
    return supportedLanguages.map { JetBrainsAcademyCourse(it) }
  }
}