package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import com.jetbrains.rider.projectView.projectTemplates.RiderProjectTemplatesAppService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.file.Path

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CSharpCourseProjectGeneratorBase(builder, course) {
  private val solutionFileName = "${course.name}.${SolutionFileType.defaultExtension}"

  override fun runInProperContext(action: suspend () -> Unit) {
    val applicationScope = RiderProjectTemplatesAppService.getInstance().scope
    applicationScope.launch(Dispatchers.Default) {
      action.invoke()
    }
  }

  override fun applySettings(projectSettings: CSharpProjectSettings) {
    super.applySettings(projectSettings)
    course.languageVersion = projectSettings.version
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> = listOf(
    EduFile(
      solutionFileName,
      GeneratorUtils.getInternalTemplateText(CSharpCourseBuilder.SOLUTION_FILE_TEMPLATE, mapOf())
    )
  )

  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    initializeSolution(it, location, solutionFileName)
  }
}