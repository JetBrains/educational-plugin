package com.jetbrains.edu.csharp

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.application
import com.jetbrains.rd.ide.model.RdOpenSolution
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.model.RdUnitTestResultData
import com.jetbrains.rider.model.RdUnitTestSession
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import com.jetbrains.rider.projectView.SolutionInitializerService
import java.nio.file.Path
import kotlin.io.path.pathString

val RdUnitTestSession.testResultData: IProperty<RdUnitTestResultData?>?
  get() = sessionOutput.valueOrNull?.resultData

fun initializeSolution(project: Project, location: Path, solutionFileName: String) {
  val description = SolutionDescriptionFactory.existing(
    "${location.pathString}/$solutionFileName", displayName = solutionFileName
  )
  val strategy = RdOpenSolution(description, true)
  application.service<SolutionInitializerService>().initSolution(project, strategy)
}