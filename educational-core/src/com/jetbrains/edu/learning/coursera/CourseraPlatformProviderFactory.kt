package com.jetbrains.edu.learning.coursera

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.coursera.newProjectUI.CourseraCoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import icons.EducationalCoreIcons
import kotlinx.coroutines.CoroutineScope
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.Icon

class CourseraPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CourseraPlatformProvider())
}

@VisibleForTesting
class CourseraPlatformProvider : CoursesPlatformProvider() {
  private val LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/coursera-assignmnets.txt"
  private val LOG = Logger.getInstance(CourseraPlatformProvider::class.java)

  override val name: String = CourseraNames.COURSERA

  override val icon: Icon get() = EducationalCoreIcons.Coursera

  override fun createPanel(scope: CoroutineScope): CoursesPanel = CourseraCoursesPanel(this, scope)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val tasks = mutableListOf<Future<Course?>>()

    for (link in getCourseLinks()) {
      tasks.add(ApplicationManager.getApplication().executeOnPooledThread(Callable {
        val tempFile = FileUtil.createTempFile("coursera-zip", null)
        DownloadUtil.downloadAtomically(null, link, tempFile)
        val courseraCourse: Course? = getCourseraCourse(tempFile.absolutePath)
        if (courseraCourse == null) {
          LOG.error("Failed to get local course from $link")
          return@Callable null
        }
        courseraCourse
      }))
    }

    val courses = tasks.mapNotNull { it.get(60, TimeUnit.SECONDS) }
      .sortedBy { it.name }

    return CoursesGroup(courses).asList()
  }

  private fun getCourseLinks(): List<String> {
    try {
      val url = URL(LINK)
      val conn = url.openConnection()
      BufferedReader(InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).use { reader ->
        return reader.readLines().asSequence().map { s: String -> s.split("#")[0].trim() }.filter { it.isNotEmpty() }.toList()
      }
    }
    catch (e: IOException) {
      LOG.warn("Failed to get courses from ${LINK}")
    }
    return emptyList()
  }

  companion object {
    @VisibleForTesting
    fun getCourseraCourse(zipPath: String): CourseraCourse? {
      val localCourse = EduUtils.getLocalCourse(zipPath) ?: return null
      return courseraCourseFromLocal(localCourse)
    }
  }

}