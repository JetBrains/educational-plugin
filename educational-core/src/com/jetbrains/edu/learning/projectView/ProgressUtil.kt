package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.jetbrains.edu.coursecreator.ui.CCCreateCoursePreviewDialog
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.submissions.SubmissionsManager
import java.awt.Color
import javax.swing.JProgressBar

object ProgressUtil {
  /**
   * @return Pair (number of solved tasks, number of tasks)
   */
  @JvmStatic
  fun countProgress(course: Course): Pair<Int, Int> {
    if (course is HyperskillCourse) {
      // we want empty progress in case project stages are not loaded
      // and only code challenges are present
      val projectLesson = course.getProjectLesson() ?: return 0 to 0
      return countProgress(projectLesson)
    }
    var taskNum = 0
    var taskSolved = 0
    course.visitLessons { lesson ->
      taskNum += lesson.taskList.size
      taskSolved += getSolvedTasks(lesson)
    }
    return Pair(taskSolved, taskNum)
  }

  @JvmStatic
  fun countProgress(lesson: Lesson): Pair<Int, Int> {
    val taskNum = lesson.taskList.size
    val taskSolved = getSolvedTasks(lesson)
    return Pair(taskSolved, taskNum)
  }

  private fun getSolvedTasks(lesson: Lesson): Int {
    return lesson.taskList
      .filter {
        val project = it.project ?: return@filter false
        it.status == CheckStatus.Solved || SubmissionsManager.getInstance(project).containsCorrectSubmission(it.id)
      }
      .count()
  }

  fun createProgressBar() : JProgressBar {
    val progressBar = JProgressBar()

    @Suppress("UsePropertyAccessSyntax") // for compatibility with JDK11
    progressBar.setUI(object : DarculaProgressBarUI() {
      override fun getRemainderColor(): Color {
        return JBColor(Gray._220, Color(76, 77, 79))
      }
    })
    progressBar.foreground = ColorProgressBar.GREEN
    progressBar.isIndeterminate = false
    progressBar.putClientProperty("ProgressBar.flatEnds", java.lang.Boolean.TRUE)
    return progressBar
  }

  @JvmStatic
  fun updateCourseProgress(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("course is null for project at ${project.basePath}")
      return
    }
    val (tasksSolved, tasksTotal) = countProgress(course)
    val pane = ProjectView.getInstance(project).currentProjectViewPane
    if (pane is CourseViewPane && EduUtils.isStudentProject(project) && !ApplicationManager.getApplication().isUnitTestMode) {
      pane.updateCourseProgress(tasksTotal, tasksSolved)
    }
    val location = project.basePath
    if (location != null && course.dataHolder.getUserData(CCCreateCoursePreviewDialog.IS_COURSE_PREVIEW_KEY) != true) {
      CoursesStorage.getInstance().updateCourseProgress(course, location, tasksSolved, tasksTotal)
    }
  }

  private val LOG: Logger = Logger.getInstance(ProgressUtil::class.java)
}
