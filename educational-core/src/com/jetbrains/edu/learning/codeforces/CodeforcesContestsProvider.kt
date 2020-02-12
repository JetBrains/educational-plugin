package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.isUnitTestMode

object CodeforcesContestsProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    checkIsBackgroundThread()
    val preferableTextLanguage = CodeforcesSettings.getInstance().codeforcesPreferableTextLanguage
    val lang = if (preferableTextLanguage != null) TaskTextLanguage.valueOf(preferableTextLanguage) else TaskTextLanguage.ENGLISH
    return if (isUnitTestMode) emptyList() else CodeforcesContestLoader.getContestInfos(locale = lang.locale)
  }
}