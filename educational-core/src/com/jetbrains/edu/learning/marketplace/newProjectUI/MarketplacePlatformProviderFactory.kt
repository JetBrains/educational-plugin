package com.jetbrains.edu.learning.marketplace.newProjectUI

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.computeUnderProgress
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseVisibility
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.api.MarketplaceConnector
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import javax.swing.Icon

class MarketplacePlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(MarketplacePlatformProvider())
}

class MarketplacePlatformProvider : CoursesPlatformProvider() {
  private val bundledCoursesNames = listOf("Kotlin Koans.zip", "Introduction to Python.zip")

  override val name: String
    get() = EduCoreBundle.message("course.dialog.marketplace")

  override val icon: Icon get() = EducationalCoreIcons.MARKETPLACE_TAB

  override fun createPanel(scope: CoroutineScope): CoursesPanel = MarketplaceCoursesPanel(this, scope)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val featuredCourseIds = stepikMarketplaceIdsMap.values
    val marketplaceCourses = MarketplaceConnector.getInstance().searchCourses()

    val marketplaceCourseNames = mutableSetOf<String>()
    for (course in marketplaceCourses) {
      marketplaceCourseNames += course.name
      if (course.id in featuredCourseIds) {
        course.visibility = CourseVisibility.FeaturedVisibility(MARKETPLACE_GROUP_ID)
      }
    }

    val bundledCourses = loadBundledCourses().filter { bundled ->
      bundled.name !in marketplaceCourseNames
    }

    val courses = (bundledCourses + marketplaceCourses).sortedBy { it.visibility }
    return CoursesGroup(courses).asList()
  }

  override fun joinAction(courseInfo: CourseInfo, courseMode: CourseMode, coursePanel: CoursePanel) {
    val course = courseInfo.course
    if (course is EduCourse && course.isMarketplace) {
      computeUnderProgress(title = EduCoreBundle.message("progress.loading.course")) {
        MarketplaceConnector.getInstance().loadCourseStructure(course)
      }
    }
    super.joinAction(courseInfo, courseMode, coursePanel)
  }

  private fun loadBundledCourses(): List<Course> {
    val courses = mutableListOf<Course>()
    for (path in getBundledCoursesPaths()) {
      val localCourse = EduUtils.getLocalCourse(path)
      if (localCourse == null) {
        LOG.error("Failed to import local course form $path")
        continue
      }
      localCourse.visibility = CourseVisibility.FeaturedVisibility(BUNDLED_GROUP_ID)
      courses.add(localCourse)
    }
    return courses
  }

  private fun getBundledCoursesPaths(): List<String> {
    return bundledCoursesNames.map { FileUtil.join(getBundledCourseRoot(it, javaClass).absolutePath, it) }
  }

  private fun getBundledCourseRoot(courseName: String, clazz: Class<*>): File {
    @NonNls val jarPath = PathUtil.getJarPathForClass(clazz)
    if (jarPath.endsWith(".jar")) {
      val jarFile = File(jarPath)
      val pluginBaseDir = jarFile.parentFile
      val coursesDir = File(pluginBaseDir, "courses")

      if (!coursesDir.exists()) {
        if (!coursesDir.mkdir()) {
          LOG.info("Failed to create courses dir")
          return coursesDir
        }
      }
      try {
        ZipUtil.extract(jarFile, pluginBaseDir) { _, name -> name == courseName }
      }
      catch (e: IOException) {
        LOG.info("Failed to extract default course", e)
      }
      return coursesDir
    }
    return File(jarPath, "courses")
  }


  companion object {
    private val LOG = Logger.getInstance(MarketplacePlatformProvider::class.java)

    // These ids are used only for sorting
    // Probably, we should use separate id for each course to provide desired sorting
    private const val BUNDLED_GROUP_ID = 0
    private const val MARKETPLACE_GROUP_ID = 1

    //Machine Learning 101 stepikId=0, marketplaceId=

    // corresponding ids for courses uploaded both to marketplace and Stepik, needed to
    // avoid courses duplication between marketplace and stepik tabs
    val stepikMarketplaceIdsMap = mapOf(238 to 16322, //Introduction to Python
                                        55498 to 16323, //Scala Tutorial
                                        59778 to 16324, //Rustlings
                                        4222 to 16325) //Kotlin Koans
  }
}