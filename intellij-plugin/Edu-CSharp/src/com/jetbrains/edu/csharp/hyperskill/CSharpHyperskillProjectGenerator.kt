package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import java.nio.file.Path

class CSharpHyperskillProjectGenerator(builder: CSharpHyperskillCourseBuilder, course: Course) :
  CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val solutionDescription = SolutionDescriptionFactory.virtual(course.name, location.toString(), listOf())
    val strategy = RdOpenSolution(solutionDescription, false)
    SolutionInitializer.initSolution(it, strategy)
  }
}