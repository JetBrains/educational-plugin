package com.jetbrains.edu.learning.coursera

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.templates.github.DownloadUtil
import com.intellij.util.ConcurrencyUtil
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseLoading.CourseLoader
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.Executors

class StartCourseraAssignment : DumbAwareAction("Start Coursera Assignment") {
  override fun actionPerformed(e: AnActionEvent) {
    val courses = CourseLoader.getCourseInfosUnderProgress {
      CoursesProvider.loadAllCourses(listOf(CourseraAssignmentsProvider))
    } ?: return
    val dialog = BrowseCoursesDialog(courses, DefaultActionGroup(ImportCourseraAssignment()))
    dialog.title = "Select Assignment"
    dialog.show()
  }

  private object CourseraAssignmentsProvider : CoursesProvider {
    private const val LINK = "https://raw.githubusercontent.com/JetBrains/educational-plugin/master/coursera-assignmnets.txt"
    private val LOG = Logger.getInstance(StartCourseraAssignment::class.java)
    private val THREAD_NUMBER = Runtime.getRuntime().availableProcessors()
    private val EXECUTOR_SERVICE = Executors.newFixedThreadPool(THREAD_NUMBER)

    override fun loadCourses(): List<Course> {
      val courses = mutableListOf<Course>()
      val tasks = mutableListOf<Callable<Course?>>()

      for (link in getCourseLinks()) {
        tasks.add(Callable {
          val tempFile = FileUtil.createTempFile("coursera-zip", null)
          DownloadUtil.downloadAtomically(null, link, tempFile)
          val courseraCourse = getCourseraCourse(tempFile.absolutePath)
          if (courseraCourse == null) {
            LOG.error("Failed to get local course from $link")
            return@Callable null
          }
          if (courseraCourse.configurator == null) {
            return@Callable null
          }
          return@Callable courseraCourse
        })
      }

      ConcurrencyUtil.invokeAll(tasks, EXECUTOR_SERVICE)
        .filterNot { it.isCancelled }
        .mapNotNull { it.get() }
        .forEach { courses.add(it) }

      return courses
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
        LOG.warn("Failed to get courses from $LINK")
      }
      return emptyList()
    }
  }

  companion object {
    @VisibleForTesting
    fun getCourseraCourse(zipPath: String): CourseraCourse? {
      val localCourse = EduUtils.getLocalCourse(zipPath) ?: return null
      return courseraCourseFromLocal(localCourse)
    }
  }
}