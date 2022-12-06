package com.jetbrains.edu.github

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.github.messages.EduGitHubBundle
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.github.PostToGithubActionProvider
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_PROJECTS_URL
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.stepik.hyperskill.notifyJBAUnauthorized
import com.jetbrains.edu.learning.stepik.hyperskill.profileUrl
import com.jetbrains.edu.learning.stepik.hyperskill.settings.HyperskillSettings
import com.jetbrains.rd.util.first
import org.jetbrains.plugins.github.GithubShareAction

class HyperskillPostToGitHubActionProvider : PostToGithubActionProvider {
  override fun isAvailable(task: Task): Boolean {
    val course = task.course
    return course is HyperskillCourse
           && task.index == course.stages.size
           && task.status == CheckStatus.Solved
  }

  override fun getAction(): AnAction = PostHyperskillProjectToGithub()

  private class PostHyperskillProjectToGithub : AnAction(EduGitHubBundle.message("hyperskill.action.post.to.github")) {
    private val readmeTemplateName = "README.md"
    private val gitignoreTemplateName = ".gitignore"

    override fun actionPerformed(e: AnActionEvent) {
      val project = e.project ?: return
      val courseDir = project.courseDir
      val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: error("HyperskillCourse expected")
      val projectName = course.getProjectLesson()?.name ?: error("Project name is null, projectId=${course.id}")

      generateReadme(project, course, projectName)
      generateGitignore(projectName, course.getProjectLesson()?.currentTask() ?: return, courseDir)
      postToGithub(project, courseDir)
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
        "projectLink" to "${HYPERSKILL_PROJECTS_URL}/${course.id}",
        "profileLink" to account.profileUrl
      )

      val fileTemplateText = GeneratorUtils.getInternalTemplateText(readmeTemplateName, templateVariables)

      GeneratorUtils.run {
        runInWriteActionAndWait {
          val readmeFile = courseDir.findOrCreateChildData(courseDir, readmeTemplateName)
          VfsUtil.saveText(readmeFile, fileTemplateText)
        }
      }
    }

    private fun postToGithub(project: Project, file: VirtualFile) {
      if (project.isDisposed) {
        return
      }

      GithubShareAction.shareProjectOnGithub(project, file)
    }

    private fun generateGitignore(projectName: String, currentTask: Task, courseDir: VirtualFile) {
      val path = currentTask.taskFiles.filter { it.value.isVisible }.first().key
      val solutionDir = if (path.contains("/")) {
        path.split("/").first() + "/"
      }
      else {
        path
      }
      val templateVariables = mapOf(
        "project_name" to projectName,
        "solution-dir" to solutionDir
      )

      GeneratorUtils.run {
        runInWriteActionAndWait {
          val fileTemplateText = getInternalTemplateText(gitignoreTemplateName, templateVariables)
          val readmeFile = courseDir.findOrCreateChildData(courseDir, gitignoreTemplateName)
          VfsUtil.saveText(readmeFile, fileTemplateText)
        }
      }
    }
  }
}