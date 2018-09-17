package com.jetbrains.edu.learning.courseLoading

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.ZipUtil
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduUtils.getLocalCourse
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.annotations.NonNls
import java.io.File
import java.io.IOException

abstract class BundledCoursesProvider : CoursesProvider {
  override fun loadCourses(): List<Course> {
    val courses = mutableListOf<Course>()
    for (path in getBundledCoursesPaths()) {
      val localCourse = getLocalCourse(path)
      if (localCourse != null) {
        LOG.error("Failed to import local course form $path")
        courses.add(localCourse)
      }
    }
    return courses
  }

  private fun getBundledCoursesPaths(): List<String> {
    return getBundledCoursesNames().map { FileUtil.join(getBundledCourseRoot(it, javaClass).absolutePath, it) }
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

  protected abstract fun getBundledCoursesNames(): List<String>

  companion object {
    private val LOG = Logger.getInstance(BundledCoursesProvider::class.java)
  }
}
