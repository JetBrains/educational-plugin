package com.jetbrains.edu.coursecreator.testGeneration.processing.manager

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestSuitePresenter
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.test.TestsPresenter

interface TestPresenterManager {

  fun getTestsPresenter(testSuitePresenter: TestSuitePresenter, testFilename: String): TestsPresenter

  fun getTestSuitePresenter(project: Project, testGenerationData: TestGenerationData): TestSuitePresenter

  companion object {
    private val EP_NAME = LanguageExtension<TestPresenterManager>("Educational.TestPresenterManager")

    fun getInstance(language: Language): TestPresenterManager = EP_NAME.forLanguage(language)
  }

}
