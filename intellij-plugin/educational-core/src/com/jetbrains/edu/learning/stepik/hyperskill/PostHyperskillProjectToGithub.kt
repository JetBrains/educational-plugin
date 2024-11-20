package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.github.PostToGithubActionProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings

class PostHyperskillProjectToGithub : AnAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (project.isDisposed) {
      return
    }

    val course = project.course as? HyperskillCourse ?: error("HyperskillCourse expected")
    val projectLesson = course.getProjectLesson() ?: error("Posting non-project lessons to GitHub is not supported")
    val projectName = projectLesson.name
    generateReadme(project, course, projectName)

    val courseDir = project.courseDir
    val currentTask = projectLesson.currentTask() ?: error("Current task not found for course with id=${course.id}")
    generateGitignore(projectName, currentTask, courseDir)

    PostToGithubActionProvider.first()?.postToGitHub(project, courseDir) ?: error(
      "PostToGithubActionProvider not found for course ${course.name}")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun generateGitignore(projectName: String, currentTask: Task, courseDir: VirtualFile) {
    val visibleTaskFiles = currentTask.taskFiles.filter { it.value.isVisible }
    val gitIgnoreString = visibleTaskFiles
      .map { it.key.substringBefore("/") }
      .filter { it != "." }
      .toSet()
      .joinToString("\n") {
        "!/$projectName/task/${it}"
      }

    val templateVariables = mapOf(
      "projectName" to projectName,
      "solutionDirs" to gitIgnoreString
    )

    GeneratorUtils.createFileFromTemplate(CourseInfoHolder.fromCourse(currentTask.course, courseDir), GITIGNORE_FILE_PATH,
                                          GITIGNORE_TEMPLATE_NAME, templateVariables)
  }

  private fun generateReadme(project: Project, course: HyperskillCourse, projectName: String) {
    val courseDir = project.courseDir

    val account = HyperskillSettings.INSTANCE.account
    if (account == null) {
      notifyJBAUnauthorized(project, EduCoreBundle.message("notification.hyperskill.cannot.post.project.to.github"))
      return
    }

    val templateVariables = mapOf(
      "projectName" to projectName,
      "projectDescription" to course.description,
      "projectLink" to "$HYPERSKILL_PROJECTS_URL/${course.id}",
      "profileLink" to account.profileUrl
    )

    GeneratorUtils.createFileFromTemplate(CourseInfoHolder.fromCourse(course, courseDir), README_FILE_PATH, README_TEMPLATE_NAME,
                                          templateVariables)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    val currentTask = project.getCurrentTask() ?: return
    e.presentation.isEnabledAndVisible = isAvailable(currentTask)
  }

  companion object {
    private const val README_TEMPLATE_NAME = "github_README.md"
    private const val README_FILE_PATH = "README.md"
    private const val GITIGNORE_TEMPLATE_NAME = "github_.gitignore"
    private const val GITIGNORE_FILE_PATH = ".gitignore"

    fun isAvailable(task: Task): Boolean {
      val course = task.course as? HyperskillCourse ?: return false
      val projectLesson = course.getProjectLesson() ?: return false
      return PostToGithubActionProvider.first() != null
             && task.lesson == projectLesson
             && task.index == course.stages.size
             && task.status == CheckStatus.Solved
    }
  }
}