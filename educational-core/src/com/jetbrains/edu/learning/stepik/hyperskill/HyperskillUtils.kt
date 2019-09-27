package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.icons.AllIcons
import com.intellij.lang.Language
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.actions.CompareWithAnswerAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.taskDescription.ui.LightColoredActionLink
import java.awt.BorderLayout
import javax.swing.JPanel

val HYPERSKILL_STAGE: Key<Int> = Key.create("HYPERSKILL_STAGE")
const val HYPERSKILL_GROUP_ID = "Hyperskill.post"

fun openSelectedStage(course: Course, project: Project) {
  val stageId = course.getUserData(HYPERSKILL_STAGE) ?: return
  if (course is HyperskillCourse && stageId > 0) {
    val index = course.stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val lesson = course.lessons[0]
      val taskList = lesson.taskList
      if (taskList.size > index) {
        val fromTask = if (lesson is FrameworkLesson) lesson.currentTask() else taskList[0]
        NavigationUtils.navigateToTask(project, taskList[index], fromTask, false)
      }
    }
  }
}

fun getTopPanelForProblem(project: Project, course: HyperskillCourse, task: Task?): JPanel? {
  if (task == null || course.isTaskInProject(task) || CCUtils.isCourseCreator(project)) {
    return null
  }
  val panel = JPanel(BorderLayout())
  panel.background = UIUtil.getListBackground()
  panel.add(LightColoredActionLink("Return to project \"${course.hyperskillProject.title}\"",
                                                                                 OpenProjectAction(project, course), AllIcons.Actions.Back), BorderLayout.WEST)
  panel.border = JBUI.Borders.empty(0, 0, 10, 0)
  return panel
}

class OpenProjectAction(private val project: Project, private val course: HyperskillCourse) : DumbAwareAction(null) {
  override fun actionPerformed(e: AnActionEvent) {
    val lesson = course.getProjectLesson() ?: return
    NavigationUtils.navigateToTask(project, lesson.currentTask())
  }
}

class HSPeekSolutionAction : CompareWithAnswerAction() {
  override fun getSolution(taskFile: TaskFile): String? {
    val task = taskFile.task
    return ProgressManager.getInstance().run(
      object : com.intellij.openapi.progress.Task.WithResult<String?, Exception>(null, "Loading solution", true) {
        override fun compute(indicator: ProgressIndicator): String? {
          return HyperskillConnector.getInstance().getSolution(task.id)?.reply
        }
      })
  }

  override fun canShowSolution(task: Task): Boolean {
    return canShowHyperskillSolution(task)
  }

  override fun getTaskFiles(task: Task) = task.taskFiles.values.toMutableList()

  companion object {
    const val ACTION_ID = "Hyperskill.PeekSolution"
  }
}

fun canShowHyperskillSolution(task: Task): Boolean {
  val course = task.course
  return course is HyperskillCourse && task.status == CheckStatus.Solved && !course.isTaskInProject(task)
}

fun stepLink(stepId: Int) = "${HYPERSKILL_URL}learn/step/$stepId"

fun isHyperskillSupportAvailable(): Boolean {
  return Language.findLanguageByID(EduNames.JAVA) != null || Language.findLanguageByID(EduNames.PYTHON) != null ||
         Language.findLanguageByID(EduNames.KOTLIN) != null
}

fun showFailedToPostNotification() {
  val notification = Notification(HYPERSKILL_GROUP_ID, "Failed to post submission to the Hyperskill",
                                  "Please, try to check again", NotificationType.WARNING)
  notification.notify(null)
}