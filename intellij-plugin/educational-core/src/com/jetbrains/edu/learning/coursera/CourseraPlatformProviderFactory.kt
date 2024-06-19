package com.jetbrains.edu.learning.coursera

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.COURSERA
import com.jetbrains.edu.learning.coursera.newProjectUI.CourseraCoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProviderFactory
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.asList
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.swing.Icon

class CourseraPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(CourseraPlatformProvider())
}

class CourseraPlatformProvider : CoursesPlatformProvider() {

  override val name: String = COURSERA

  override val icon: Icon get() = EducationalCoreIcons.Coursera

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = CourseraCoursesPanel(this, scope, disposable)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val tasks = mutableListOf<Future<Course?>>()

    for (link in getCourseLinks()) {
      tasks.add(ApplicationManager.getApplication().executeOnPooledThread(Callable {
        val tempFile = FileUtil.createTempFile("coursera-zip", null)
        DownloadUtil.downloadAtomically(null, link, tempFile)
        val courseraCourse = EduUtilsKt.getCourseraCourse(tempFile.absolutePath)
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
    return try {
      val url = URL(LINK)
      val conn = url.openConnection()
      conn.getInputStream().bufferedReader().useLines { lines ->
        lines.map { s -> s.split("#")[0].trim() }.filter { it.isNotEmpty() }.toList()
      }
    }
    catch (e: IOException) {
      LOG.warn("Failed to get courses from $LINK")
      emptyList()
    }
  }


  companion object {
    private const val LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/coursera-assignments.txt"
    private val LOG = logger<CourseraPlatformProvider>()
  }
}
