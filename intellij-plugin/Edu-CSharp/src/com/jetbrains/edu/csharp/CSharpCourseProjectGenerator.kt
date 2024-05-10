package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import com.jetbrains.rider.projectView.*
import java.nio.file.Path
import kotlin.io.path.pathString

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  private val solutionFileName = "${course.name}.${SolutionFileType.defaultExtension}"
  override fun applySettings(projectSettings: CSharpProjectSettings) {
    super.applySettings(projectSettings)
    course.languageVersion = projectSettings.version
  }

  override fun afterProjectGenerated(project: Project, projectSettings: CSharpProjectSettings, onConfigurationFinished: () -> Unit) {
    super.afterProjectGenerated(project, projectSettings, onConfigurationFinished)
    CSharpCourseBuilder.addCSProjectToSolution(project, project.courseDir.findChild("lesson1")?.findChild("task1")?.findChild("task1.csproj")?.path ?: return)
    // addCSProjectToSolution добавляет первую таску в .sln файл (в первый раз внутри initNewTask не получится, потому что там project еще null)
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    if (!isNewCourse) return
    val content = GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE, mapOf())

    GeneratorUtils.createTextChildFile(
      holder, holder.courseDir,
      solutionFileName,
      content
    )
  }

  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val description = SolutionDescriptionFactory.existing(
      "${location.pathString}/$solutionFileName"
    )
    val strategy = RdOpenSolution(description, false)
    SolutionInitializer.initSolution(it, strategy)
  }
}