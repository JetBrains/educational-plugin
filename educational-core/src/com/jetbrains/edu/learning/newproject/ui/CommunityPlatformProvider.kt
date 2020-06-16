package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.StepikCoursesProvider
import com.jetbrains.edu.learning.stepik.featuredCourses
import icons.EducationalCoreIcons
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException
import javax.swing.Icon

class CommunityPlatformProvider(private val coursesProvider: StepikCoursesProvider) : CoursesPlatformProvider {
  private val bundledCoursesNames = listOf("Kotlin Koans.zip", "Introduction to Python.zip")

  override val name: String
    get() = EduCoreBundle.message("course.dialog.community.courses")

  override val icon: Icon get() = EducationalCoreIcons.CommunityCourses

  override val panel: CoursesPanel get() = CommunityCoursesPanel(this)

  override suspend fun loadCourses(): List<Course> {
    val communityCourses = coursesProvider.getCommunityCourses()

    val featuredCourses = communityCourses.filter { it.id in featuredCourses }
    val bundledCourses = loadBundledCourses().filter { bundled ->
      featuredCourses.none { featured ->
        featured.name != bundled.name
      }
    }
    return communityCourses.plus(bundledCourses)
  }

  private fun loadBundledCourses(): List<Course> {
    val courses = mutableListOf<Course>()
    for (path in getBundledCoursesPaths()) {
      val localCourse = EduUtils.getLocalCourse(path)
      if (localCourse == null) {
        LOG.error("Failed to import local course form $path")
        continue
      }
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
    private val LOG = Logger.getInstance(CommunityPlatformProvider::class.java)
  }
}