package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.actions.studyItem.NewStudyItemInfo
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.intellij.openapi.util.Key
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rider.model.AddProjectCommand
import com.jetbrains.rider.model.projectModelTasks
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.projectView.workspace.getId
import com.jetbrains.rider.projectView.workspace.getSolutionEntity
import com.jetbrains.rider.util.idea.runCommandUnderProgress


class CSharpCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun taskTemplateName(course: Course): String = CSharpConfigurator.TASK_CS

  override fun testTemplateName(course: Course): String = CSharpConfigurator.TEST_CS
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getDefaultSettings(): Result<CSharpProjectSettings, String> = Ok(CSharpProjectSettings())

  override fun initNewTask(course: Course, task: Task, info: NewStudyItemInfo, withSources: Boolean) {
    val version = course.languageVersion

    task.addTaskFile(
      TaskFile(
        "${task.name}.csproj",
        GeneratorUtils.getInternalTemplateText(PROJECT_FILE_TEMPLATE, mapOf(VERSION_VARIABLE to getDotNetVersion(version)))
      )
    )
    // вот сразу после этой строчки файла csproj еще нет на диске, поэтому addCSProjectToSolution не находит путь
    course.project?.let {
      addCSProjectToSolution(it, "${it.basePath}/lesson1/${task.dirName}/${task.name}.csproj")
      // если захочешь посмотреть, как работает addCSProjectToSolution, раскомменть строчку ниже
      // addCSProjectToSolution(it, "${it.basePath}/lesson1/task1/task1.csproj")
      // тогда при добавлении новой таски csproj для первой таски добавится в sln
      // (это все в демонстрационных целях и в идеале должно бы проходить в afterProjectGenerated, но нет)
    }

    super.initNewTask(course, task, info, withSources)
  }
  companion object {
    const val PROJECT_FILE_TEMPLATE = "Project.csproj"
    const val TEST_PROJECT_FILE_TEMPLATE = "Test.csproj" // пока что не юзается, будет актуально, если все таки будут отдельно src и test папки
    const val SOLUTION_FILE_TEMPLATE = "Solution.sln"
    const val VERSION_VARIABLE = "VERSION"

    fun addCSProjectToSolution(project: Project, path: String) { // штука чтобы экспандить sln файл, когда добавляется новый csproj
      val tasks = project.solution.projectModelTasks
      val parentId = WorkspaceModel.getInstance(project).getSolutionEntity()?.getId(project) ?: return // getId падает в первый раз
      val parameters = RdPostProcessParameters(false, listOf())
      val command = AddProjectCommand(parentId, listOf(path), listOf(), true, parameters)
      tasks.addProject.runCommandUnderProgress(command, project, "Adding new project") // это потом будет bundle
    }
  }
}
