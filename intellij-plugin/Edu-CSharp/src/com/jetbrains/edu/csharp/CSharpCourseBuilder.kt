package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.jetbrains.edu.coursecreator.actions.TemplateFileInfo
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rdclient.util.idea.toIOFile
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework


class CSharpCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun taskTemplateName(course: Course): String = CSharpConfigurator.TASK_CS

  override fun testTemplateName(course: Course): String = CSharpConfigurator.TEST_CS

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getDefaultSettings(): Result<CSharpProjectSettings, String> = Ok(CSharpProjectSettings())

  override fun initNewTask(course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean) {
    info.putUserData(CSPROJ_NAME_PER_TASK_KEY, task.getCSProjFileNameWithoutExtension())
    super.initNewTask(course, task, info, withSources)
  }

  override fun shouldCopyTaskFile(path: String): Boolean {
    return !path.endsWith(CsprojFileType.defaultExtension)
  }

  override fun beforeStudyItemDeletion(project: Project, item: StudyItem) {
    super.beforeStudyItemDeletion(project, item)
    when (item) {
      is Task -> CSharpBackendService.getInstance(project).removeCSProjectFilesFromSolution(listOf(item))
      is Lesson -> {
        CSharpBackendService.getInstance(project).removeCSProjectFilesFromSolution(item.taskList)
        val parentDir = item.getDir(project.courseDir)?.parent ?: return
        if (parentDir == project.courseDir) {
          CSharpBackendService.getInstance(project).excludeFilesFromCourseView(listOfNotNull(parentDir.toIOFile()))
        }
      }

      is Section -> {
        CSharpBackendService.getInstance(project).removeCSProjectFilesFromSolution(item.lessons.flatMap { it.taskList })
        CSharpBackendService.getInstance(project).excludeFilesFromCourseView(listOfNotNull(item.getDir(project.courseDir)?.toIOFile()))
      }
    }
  }

  override fun onStudyItemCreation(project: Project, item: StudyItem) {
    super.onStudyItemCreation(project, item)
    when (item) {
      is Task -> CSharpBackendService.getInstance(project).addCSProjectFilesToSolution(listOf(item))
    }
  }

  override fun extractInitializationParams(info: NewStudyItemInfo): Map<String, String> =
    mutableMapOf(VERSION_VARIABLE to getDotNetVersion(info.getUserData(VERSION_KEY)))

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = super.getDefaultTaskTemplates(course, info, withSources, withTests).toMutableList()
    templates.add(getCSProjTemplate(course, info))
    return templates
  }

  private fun getCSProjTemplate(course: Course, info: NewStudyItemInfo): TemplateFileInfo {
    val csprojNameNoExtension = info.getUserData(CSPROJ_NAME_PER_TASK_KEY) ?: error("CSProj filename not found")
    info.putUserData(VERSION_KEY, course.languageVersion)
    return TemplateFileInfo(PROJECT_FILE_TEMPLATE, "$csprojNameNoExtension.${CsprojFileType.defaultExtension}", false)
  }

  override fun getSupportedLanguageVersions(): List<String> = ProjectTemplateTargetFramework.allPredefinedNet.map { it.presentation }

  companion object {
    const val PROJECT_FILE_TEMPLATE = "ProjectWithTests.csproj"
    const val SOLUTION_FILE_TEMPLATE = "Solution.sln"
    const val VERSION_VARIABLE = "VERSION"

    val VERSION_KEY = Key.create<String>("edu.csharp.dotnetVersion")
    val CSPROJ_NAME_PER_TASK_KEY = Key.create<String>("edu.csharp.csproj.path")
  }
}