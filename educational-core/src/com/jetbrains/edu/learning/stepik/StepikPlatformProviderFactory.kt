package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.newproject.ui.CommunityPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.JetBrainsAcademyPlatformProvider
import com.jetbrains.edu.learning.stepik.newProjectUI.StepikPlatformProvider

/**
 * Lists providers for courses that are stored on Stepik: [StepikCourse]s, [EduCourse]s created in the plugin,
 * and  maybe [HyperskillCourse] in the future.
 * All courses are loaded once by [StepikCoursesProvider] and each [CoursesPlatformProvider] filters the needed.
 */
class StepikPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> {
    val coursesProvider = StepikCoursesProvider()
    return listOf(
      JetBrainsAcademyPlatformProvider(),
      StepikPlatformProvider(coursesProvider),
      CommunityPlatformProvider(coursesProvider)
    )
  }
}