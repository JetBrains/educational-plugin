package com.jetbrains.edu.java.testGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.processing.manager.TestPresenterManager
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestSuitePresenter
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.test.TestsPresenter
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM

class JavaTestPresenterManager : TestPresenterManager {
  override fun getTestsPresenter(testSuitePresenter: TestSuitePresenter, testFilename: String) = object : TestsPresenter {
    override fun representTestSuite(testSuite: TestSuiteGeneratedByLLM): String {
      return testSuitePresenter.toStringWithoutExpectedException(testSuite, testFilename)
    }

    override fun representTestCase(testSuite: TestSuiteGeneratedByLLM, testCaseIndex: Int): String {
      return testSuitePresenter.toStringSingleTestCaseWithoutExpectedException(testSuite, testCaseIndex)
    }
  }

  override fun getTestSuitePresenter(project: Project, testGenerationData: TestGenerationData): TestSuitePresenter {
    return JUnitTestSuitePresenter(project, testGenerationData)
  }
}
