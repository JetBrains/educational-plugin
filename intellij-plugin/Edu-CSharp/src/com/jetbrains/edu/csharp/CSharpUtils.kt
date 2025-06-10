package com.jetbrains.edu.csharp

import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.csharp.messages.EduCSharpBundle
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.getLesson
import com.jetbrains.edu.learning.getSection
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.environmentSetup.EnvironmentSetupConfigurable
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.findProjectsByName
import com.jetbrains.rider.projectView.workspace.getSolutionEntity

val DEFAULT_DOT_NET = ProjectTemplateTargetFramework.latest.presentation

fun Task.csProjPathByTask(project: Project): String = GeneratorUtils.joinPaths(getDir(project.courseDir)?.path, getCSProjFileName())

fun getDotNetVersion(version: String?): String = version ?: DEFAULT_DOT_NET

fun Task.getCSProjFileName(): String = "${getCSProjFileNameWithoutExtension()}.${CsprojFileType.defaultExtension}"

fun Task.getCSProjFileNameWithoutExtension(): String = pathInCourse.formatForCSProj()

fun String.formatForCSProj(): String = split("/").joinToString(".") { it.capitalize() }

fun Task.getTestName(): String = getCSProjFileNameWithoutExtension().toTestName()

fun String.toTestName(): String = filter { it != '.' } + "Test"
fun Task.toProjectModelEntity(project: Project): ProjectModelEntity? = WorkspaceModel.getInstance(project).findProjectsByName(getCSProjFileNameWithoutExtension()).firstOrNull()

fun Project.getSolutionEntity(): ProjectModelEntity? = WorkspaceModel.getInstance(this).getSolutionEntity()

fun includeTopLevelDirsInCourseView(project: Project, course: Course) {
  val filesToIndex = project.courseDir.children.filter { it.isTopLevelDirectory(project, course) }.mapNotNull { it.toIOFile() }
  CSharpBackendService.getInstance(project).includeFilesToCourseView(filesToIndex)
}

fun VirtualFile.isTopLevelDirectory(project: Project, course: Course): Boolean {
  return getSection(project) != null || getLesson(project) != null
         || course.configurator?.shouldFileBeVisibleToStudent(this) == true
}

fun showInstallMSBuildNotification(project: Project) {
  EduNotificationManager.create(
    ERROR,
    EduCSharpBundle.message("notification.title.check.net.installed"),
    EduCSharpBundle.message("notification.content.net.required.to.add.c.projects.to.solution")
  ).apply {
    addAction(object : AnAction(EduCSharpBundle.message("action.open.environment.settings.text")) {
      override fun actionPerformed(e: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, EnvironmentSetupConfigurable::class.java)
      }
    }).notify(project)
  }
}