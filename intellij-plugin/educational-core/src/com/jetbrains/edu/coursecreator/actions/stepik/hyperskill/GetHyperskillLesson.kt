package com.jetbrains.edu.coursecreator.actions.stepik.hyperskill

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.coursecreator.ui.CCNewCourseDialog
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.isFeatureEnabled
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.StepSource
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikTaskBuilder
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.loadAndFillAdditionalCourseInfo
import com.jetbrains.edu.learning.stepik.api.loadAndFillLessonAdditionalInfo
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

class GetHyperskillLesson : DumbAwareAction(
  EduCoreBundle.lazyMessage("action.get.lesson.text", HYPERSKILL, StepikNames.STEPIK),
  EduCoreBundle.lazyMessage("action.get.lesson.description", HYPERSKILL, StepikNames.STEPIK),
  EducationalCoreIcons.Platform.JetBrainsAcademy
) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isFeatureEnabled(EduExperimentalFeatures.CC_HYPERSKILL)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val lessonId = Messages.showInputDialog(
      EduCoreBundle.message("action.get.lesson.enter.lesson.id"),
      EduCoreBundle.message("action.get.lesson.text", HYPERSKILL, StepikNames.STEPIK),
      EducationalCoreIcons.Platform.JetBrainsAcademy
    )
    if (!lessonId.isNullOrEmpty()) {
      ProgressManager.getInstance().run(object : com.intellij.openapi.progress.Task.Modal(
        project,
        EduCoreBundle.message("action.get.course.loading"),
        true
      ) {
        override fun run(indicator: ProgressIndicator) {
          val course = createCourse(lessonId) ?: return
          val configurator = course.configurator
          if (configurator == null) {
            val environment = if (course.environment == DEFAULT_ENVIRONMENT) "default" else course.environment
            showError(
              EduCoreBundle.message("error.failed.to.create.lesson.no.configuration", course.languageId, environment),
              EduCoreBundle.message("error.failed.to.create.lesson")
            )
            return
          }
          runInEdt {
            CCNewCourseDialog(
              EduCoreBundle.message("action.get.lesson.text", HYPERSKILL, StepikNames.STEPIK),
              EduCoreBundle.message("label.create"),
              course
            ).show()
          }
        }
      })
    }
  }

  companion object {
    @VisibleForTesting
    fun createCourse(lessonId: String): HyperskillCourse? {
      val course = HyperskillCourse()
      val lesson = StepikConnector.getInstance().getLesson(Integer.valueOf(lessonId))
      if (lesson == null) {
        showIncorrectCredentialsError()
        return null
      }
      val allStepSources = StepikConnector.getInstance().getStepSources(lesson.stepIds)
      val tasks = getTasks(course, allStepSources)
      for (task in tasks) {
        lesson.addTask(task)
      }

      val languageAndEnvironment = getLanguageAndEnvironment(lesson)

      @NonNls val hyperskillLessonName = "Hyperskill lesson $lessonId"
      course.apply {
        name = hyperskillLessonName
        description = hyperskillLessonName
        languageId = languageAndEnvironment.first
        environment = languageAndEnvironment.second
      }

      lesson.parent = course
      val hyperskillLesson = FrameworkLesson(lesson)
      course.addItem(0, hyperskillLesson)
      loadAndFillAdditionalCourseInfo(course)
      loadAndFillLessonAdditionalInfo(lesson, course)

      return course
    }

    private fun showIncorrectCredentialsError() {
      val stepikUser = EduSettings.getInstance().user

      val message = if (stepikUser == null) {
        EduCoreBundle.message("error.failed.to.get.lesson.not.log.in", StepikNames.STEPIK)
      }
      else {
        EduCoreBundle.message("error.failed.to.get.lesson.no.access", StepikNames.STEPIK, stepikUser.name)
      }
      showError(message, EduCoreBundle.message("error.failed.to.get.lesson"))
    }

    private fun getTasks(course: Course, allStepSources: List<StepSource>): List<Task> =
      allStepSources.map { step ->
        val builder = StepikTaskBuilder(course, step)
        val type = step.block?.name ?: error("Can't get type from step source")
        builder.createTask(type)
      }

    @Suppress("UnstableApiUsage")
    private fun showError(
      @NlsContexts.DialogMessage message: String,
      @NlsContexts.DialogTitle title: String
    ) {
      runInEdt {
        Messages.showErrorDialog(message, title)
      }
    }

    private fun getLanguageAndEnvironment(lesson: Lesson): Pair<String, String> {
      for (task in lesson.taskList) {
        val taskFiles = task.taskFiles.values
        if (taskFiles.any { it.name.contains("androidTest") || it.name.contains("AndroidManifest.xml") }) {
          return EduFormatNames.KOTLIN to EduNames.ANDROID
        }
        if (taskFiles.any { it.name == "tests.py" }) {
          return EduFormatNames.PYTHON to DEFAULT_ENVIRONMENT
        }
        for (taskFile in taskFiles) {
          if (!taskFile.isVisible) {
            continue
          }
          val languageAndEnvironment = when (FileUtilRt.getExtension(taskFile.name)) {
            "java" -> EduFormatNames.JAVA to DEFAULT_ENVIRONMENT
            "py" -> EduFormatNames.PYTHON to EduNames.UNITTEST
            "kt" -> EduFormatNames.KOTLIN to DEFAULT_ENVIRONMENT
            "js", "html" -> EduFormatNames.JAVASCRIPT to DEFAULT_ENVIRONMENT
            "scala" -> EduFormatNames.SCALA to DEFAULT_ENVIRONMENT
            else -> null
          }

          if (languageAndEnvironment != null) {
            return languageAndEnvironment
          }
        }
      }
      return EduFormatNames.JAVA to DEFAULT_ENVIRONMENT
    }
  }
}
