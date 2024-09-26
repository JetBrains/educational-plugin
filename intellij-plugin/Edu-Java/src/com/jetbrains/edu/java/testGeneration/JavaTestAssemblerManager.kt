package com.jetbrains.edu.java.testGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.processing.manager.TestAssemblerManager
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestRequestedAssembler
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator

class JavaTestAssemblerManager : TestAssemblerManager {
  override fun getTestAssembler(
    project: Project,
    indicator: CustomProgressIndicator,
    generationData: TestGenerationData
  ): TestRequestedAssembler {
    return JUnitTestsAssembler(project, indicator, generationData)
  }
}