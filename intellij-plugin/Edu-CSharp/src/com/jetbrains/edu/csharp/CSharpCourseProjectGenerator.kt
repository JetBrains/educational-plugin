package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.CourseInfoHolder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.ideaInterop.fileTypes.sln.SolutionFileType
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import java.nio.file.Path
import kotlin.io.path.pathString

class CSharpCourseProjectGenerator(
  builder: CSharpCourseBuilder,
  course: Course
) : CSharpCourseProjectGeneratorBase(builder, course) {
  private val solutionFileName = "${course.name}.${SolutionFileType.defaultExtension}"

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
    val description = SolutionDescriptionFactory.existing(
      "${location.pathString}/$solutionFileName"
    )
    val strategy = RdOpenSolution(description, true)
    SolutionInitializer.initSolution(it, strategy)
  }
}