package com.jetbrains.edu.csharp

import com.jetbrains.rd.ide.model.RdExistingSolution
import com.jetbrains.rd.util.reactive.IProperty
import com.jetbrains.rider.model.RdUnitTestResultData
import com.jetbrains.rider.model.RdUnitTestSession
import com.jetbrains.rider.projectView.SolutionDescriptionFactory
import java.nio.file.Path
import kotlin.io.path.pathString

fun getExistingSolution(location: Path, solutionFileName: String): RdExistingSolution {
  return SolutionDescriptionFactory.existing(
    "${location.pathString}/$solutionFileName", displayName = solutionFileName
  )
}

val RdUnitTestSession.testResultData: IProperty<RdUnitTestResultData?>?
  get() = sessionOutput.valueOrNull?.resultData