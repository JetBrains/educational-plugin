package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.csharp.CSharpCourseProjectGeneratorBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import java.nio.file.Path
import kotlin.io.path.name

class CSharpHyperskillProjectGenerator(builder: CSharpHyperskillCourseBuilder, course: Course) :
  CSharpCourseProjectGeneratorBase(builder, course) {
  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val solutionDescription = SolutionDescriptionFactory.virtual(location.name, location.toString(), listOf())
    val strategy = RdOpenSolution(solutionDescription, false)
    SolutionInitializer.initSolution(it, strategy)
  }
}