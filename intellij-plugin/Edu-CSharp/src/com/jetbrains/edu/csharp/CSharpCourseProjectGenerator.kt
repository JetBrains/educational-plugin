package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.*
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateSdk
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework
import java.nio.file.Path
import kotlin.io.path.pathString

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CourseProjectGenerator<CSharpProjectSettings>(builder, course) {

  private val projectFileName = "${course.name}.${CsprojFileType.defaultExtension}"
  override fun applySettings(projectSettings: CSharpProjectSettings) {
    super.applySettings(projectSettings)
    course.languageVersion = projectSettings.version
  }

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>, isNewCourse: Boolean) {
    if (!isNewCourse) return
    GeneratorUtils.createFileFromTemplate(
      holder,
      holder.courseDir,
      projectFileName,
      PROJECT_FILE_TEMPLATE,
      mapOf(VERSION_VARIABLE to getDotNetVersion(course.languageVersion))
    )
  }

  private fun getDotNetVersion(version: String?): String = when (version) { // more versions to be added
    ProjectTemplateSdk.net7.presentation -> ProjectTemplateTargetFramework.net70.presentation
    else -> ProjectTemplateTargetFramework.net80.presentation
  }

  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val description = SolutionDescriptionFactory.virtual(
      location.pathString,
      listOf("${location.pathString}/${projectFileName}")
    )
    val strategy = RdOpenSolution(description, false)
    SolutionInitializer.initSolution(it, strategy)
  }

  companion object {
    private const val PROJECT_FILE_TEMPLATE = "Project.csproj"
    private const val VERSION_VARIABLE = "VERSION"
  }
}