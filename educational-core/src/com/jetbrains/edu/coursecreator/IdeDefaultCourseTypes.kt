@file:JvmName("IdeDefaultCourseTypes")
package com.jetbrains.edu.coursecreator

import com.intellij.lang.Language
import com.intellij.util.PlatformUtils
import com.jetbrains.edu.coursecreator.ui.CCNewCoursePanel
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtilsKt

fun getDefaultCourseType(courses: List<CCNewCoursePanel.CourseData>): CCNewCoursePanel.CourseData? =
  courses.find { it.language == Language.findLanguageByID(getDefaultLanguageId()) } ?: courses.firstOrNull()

fun getDefaultLanguageId(): String? = when {
  PlatformUtils.isIntelliJ() || EduUtilsKt.isAndroidStudio() -> EduNames.KOTLIN
  PlatformUtils.isPyCharm() -> EduNames.PYTHON
  PlatformUtils.isWebStorm() -> EduNames.JAVASCRIPT
  PlatformUtils.isCLion() -> EduNames.CPP
  PlatformUtils.isGoIde() -> EduNames.GO
  PlatformUtils.isPhpStorm() -> EduNames.PHP
  else -> null
}


