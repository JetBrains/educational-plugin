package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.isPreview
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import java.awt.Color
import javax.swing.JProgressBar

object ProgressUtil {

  fun countProgress(course: Course): CourseProgress {
    if (course is HyperskillCourse) {
      // we want empty progress in case project stages are not loaded
      // and only code challenges are present
      val projectLesson = course.getProjectLesson() ?: return CourseProgress(0, 0)
      return countProgress(projectLesson)
    }
    var taskNum = 0
    var taskSolved = 0
    course.visitLessons { lesson ->
      if (lesson is FrameworkLesson) {
        taskNum++
        if (lesson.taskList.all { it.status == CheckStatus.Solved }) {
          taskSolved++
        }
      }
      else {
        taskNum += lesson.taskList.size
        taskSolved += getSolvedTasks(lesson)
      }
    }
    return CourseProgress(taskSolved, taskNum)
  }

  fun countProgress(lesson: Lesson): CourseProgress {
    val taskNum = lesson.taskList.size
    val taskSolved = getSolvedTasks(lesson)
    return CourseProgress(taskSolved, taskNum)
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

    progressBar.setUI(object : DarculaProgressBarUI() {
      override fun getRemainderColor(): Color {
        return JBColor(Gray._220, Color(76, 77, 79))
      }
    })
    progressBar.foreground = ColorProgressBar.GREEN
    progressBar.isIndeterminate = false
    progressBar.putClientProperty("ProgressBar.flatEnds", true)
    return progressBar
  }

  fun updateCourseProgress(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("course is null for project at ${project.basePath}")
      return
    }
    val progress = countProgress(course)
    val pane = ProjectView.getInstance(project).currentProjectViewPane
    if (pane is CourseViewPane && project.isStudentProject() && !ApplicationManager.getApplication().isUnitTestMode) {
      pane.updateCourseProgress(progress)
    }
    val location = project.basePath
    if (location != null && !course.isPreview) {
      CoursesStorage.getInstance().updateCourseProgress(course, location, progress.tasksSolved, progress.tasksTotalNum)
    }
  }

  private val LOG: Logger = Logger.getInstance(ProgressUtil::class.java)

  data class CourseProgress(val tasksSolved: Int, val tasksTotalNum: Int)
}
