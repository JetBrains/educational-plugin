package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.model.RdUnitTestResultData
import com.jetbrains.rider.model.RdUnitTestSession
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializer
import java.nio.file.Path
import kotlin.io.path.pathString

val RdUnitTestSession.testResultData: IProperty<RdUnitTestResultData?>?
  get() = resultData

fun initializeSolution(project: Project, location: Path, solutionFileName: String) {
  val description = SolutionDescriptionFactory.existing(
    "${location.pathString}/$solutionFileName"
  )
  val strategy = RdOpenSolution(description, true)
  SolutionInitializer.initSolution(project, strategy)
}