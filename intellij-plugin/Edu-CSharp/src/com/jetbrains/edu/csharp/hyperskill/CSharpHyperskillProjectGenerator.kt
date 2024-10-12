package com.jetbrains.edu.csharp.hyperskill

import com.jetbrains.edu.csharp.CSharpProjectSettings
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rd.ide.model.RdTemporarySolution
import com.jetbrains.rider.projectView.SolutionInitializer
import java.nio.file.Path

class CSharpHyperskillProjectGenerator(builder: CSharpHyperskillCourseBuilder, course: Course) :
  CourseProjectGenerator<CSharpProjectSettings>(builder, course) {
  override fun beforeInitHandler(location: Path): BeforeInitHandler = BeforeInitHandler {
    val solutionDescription = RdTemporarySolution(course.name, location.toString(), null)
    val strategy = RdOpenSolution(solutionDescription, false)
    SolutionInitializer.initSolution(it, strategy)
  }
}