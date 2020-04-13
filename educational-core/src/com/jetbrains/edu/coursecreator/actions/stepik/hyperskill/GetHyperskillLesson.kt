package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.actions.CCPluginToggleAction
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.StepikCourseLoader
import com.jetbrains.edu.learning.stepik.api.loadAndFillAdditionalCourseInfo
import com.jetbrains.edu.learning.stepik.api.loadAndFillLessonAdditionalInfo
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import icons.EducationalCoreIcons

@Suppress("ComponentNotRegistered") // Hyperskill.xml
class GetHyperskillLesson : DumbAwareAction("Get Hyperskill Lesson from Stepik", "Get Hyperskill Lesson from Stepik",
                                            EducationalCoreIcons.JB_ACADEMY_ENABLED) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = CCPluginToggleAction.isCourseCreatorFeaturesEnabled
                                         && isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val lessonId = Messages.showInputDialog("Please, enter lesson id", "Get Hyperskill Lesson from Stepik", EducationalCoreIcons.JB_ACADEMY_ENABLED)
    if (lessonId != null && lessonId.isNotEmpty()) {
      ProgressManager.getInstance().run(object : Task.Modal(project, "Loading Course", true) {
        override fun run(indicator: ProgressIndicator) {
          val course = createCourse(lessonId) ?: return
          runInEdt {
            CCNewCourseDialog("Get Hyperskill Lesson from Stepik", "Create", course).show()
          }
        }
      })
    }
  }

  @VisibleForTesting
  fun createCourse(lessonId: String): HyperskillCourse? {
    val course = HyperskillCourse()
    val lesson = StepikConnector.getInstance().getLesson(Integer.valueOf(lessonId))
    if (lesson == null) {
      showErrorMessage()
      return null
    }
    val allStepSources = StepikConnector.getInstance().getStepSources(lesson.steps)
    val tasks = StepikCourseLoader.getTasks(course, lesson, allStepSources)
    for (task in tasks) {
      lesson.addTask(task)
    }

    course.name = "Hyperskill lesson $lessonId"
    course.description = "Hyperskill lesson $lessonId"
    course.language = getLanguage(lesson)

    val hyperskillLesson = FrameworkLesson(lesson)
    course.addItem(hyperskillLesson, 0)
    loadAndFillAdditionalCourseInfo(course)
    loadAndFillLessonAdditionalInfo(lesson, course)

    return course
  }

  private fun showErrorMessage() {
    val stepikUser = EduSettings.getInstance().user

    val message = if (stepikUser == null) {
      "Log in to ${StepikNames.STEPIK} in Settings/Education to access the lesson"
    }
    else {
      "Check that ${StepikNames.STEPIK} account `${stepikUser.name}` has access to the lesson"
    }

    runInEdt {
      Messages.showErrorDialog(message, "Failed to Get Lesson")
    }
  }

  private fun getLanguage(lesson: Lesson): String {
    for (task in lesson.taskList) {
      for (taskFile in task.taskFiles.values) {
        if (!taskFile.isVisible) {
          continue
        }
        val extension = FileUtilRt.getExtension(taskFile.name)
        if (extension == "py")
          return EduNames.PYTHON
        if (extension == "kt") {
          return EduNames.KOTLIN
        }
        if (extension == "js" || extension == "html") {
          return EduNames.JAVASCRIPT
        }
      }
    }
    return EduNames.JAVA
  }
}