package com.jetbrains.edu.learning.stepik.newProjectUI

import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.marketplace.newProjectUI.MarketplacePlatformProvider.Companion.stepikMarketplaceIdsMap
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import javax.swing.Icon

class StepikPlatformProvider : CoursesPlatformProvider() {

  private val coursesProvider: StepikCoursesProvider = StepikCoursesProvider()

  override val name: String = StepikNames.STEPIK

  override val icon: Icon get() = EducationalCoreIcons.StepikCourseTab

  override fun createPanel(scope: CoroutineScope): CoursesPanel = StepikCoursesPanel(this, coursesProvider, scope)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    checkIsBackgroundThread()
    return if (isUnitTestMode) {
      emptyList()
    }
    else {
      val groups = mutableListOf<CoursesGroup>()

      val privateCourses = coursesProvider.getPrivateCourses()
      if (privateCourses.isNotEmpty()) {
        groups.add(CoursesGroup(EduCoreBundle.message("course.dialog.private.courses.group"), privateCourses))
      }

      // we don't duplicate courses uploaded to marketplace if they were not started already
      val featuredCourses = coursesProvider.getFeaturedCourses().filter {
        CoursesStorage.getInstance().hasCourse(it) || it.id !in stepikMarketplaceIdsMap
      }

      val stepikAndEduCourses = (coursesProvider.getStepikCourses() + coursesProvider.getAllOtherCourses())
        .sortedByDescending { it.reviewScore }

      val coursesGroupName = if (privateCourses.isNotEmpty()) EduCoreBundle.message("course.dialog.public.courses.group") else ""
      groups.add(CoursesGroup(coursesGroupName, featuredCourses + stepikAndEduCourses))
      return groups
    }
  }
}