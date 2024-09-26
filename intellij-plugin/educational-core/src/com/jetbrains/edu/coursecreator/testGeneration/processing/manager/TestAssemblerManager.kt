package com.jetbrains.edu.coursecreator.testGeneration.processing.manager

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestRequestedAssembler
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator

interface TestAssemblerManager {

  fun getTestAssembler(project: Project, indicator: CustomProgressIndicator, generationData: TestGenerationData): TestRequestedAssembler

  companion object {
    private val EP_NAME = LanguageExtension<TestAssemblerManager>("Educational.TestAssemblerManager")

    fun getInstance(language: Language): TestAssemblerManager = EP_NAME.forLanguage(language)
  }
}