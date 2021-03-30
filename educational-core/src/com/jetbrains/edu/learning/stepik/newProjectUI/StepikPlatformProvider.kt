package com.jetbrains.edu.learning.stepik.newProjectUI

import com.jetbrains.edu.learning.checkIsBackgroundThread
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
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

  override suspend fun loadCourses(): List<CoursesGroup> {
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

      val stepikCourses = coursesProvider.getStepikCourses()
        .filter {
          val compatibility = it.compatibility
          compatibility == CourseCompatibility.Compatible || compatibility is CourseCompatibility.PluginsRequired
        }
        .sortedBy { it.name }
      if (stepikCourses.isNotEmpty()) {
        groups.add(CoursesGroup(EduCoreBundle.message("course.dialog.courses", name), stepikCourses))
      }

      // we don't duplicate courses uploaded to marketplace if they were not started already
      val featuredCourses = coursesProvider.getFeaturedCourses().filter {
        CoursesStorage.getInstance().hasCourse(it) || it.id !in stepikMarketplaceIdsMap
      }

      val communityCourses = featuredCourses + coursesProvider.getAllOtherCourses()
      if (communityCourses.isNotEmpty()) {
        groups.add(CoursesGroup(EduCoreBundle.message("course.dialog.community.courses"), communityCourses))
      }

      return groups
    }
  }
}