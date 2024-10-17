package com.jetbrains.edu.java.testGeneration

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestSuitePresenter
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.getClassWithTestCaseName
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM

class JUnitTestSuitePresenter(
  private val project: Project,
  private val generatedTestsData: TestGenerationData,
) : TestSuitePresenter {

  override fun toString(testSuite: TestSuiteGeneratedByLLM, testFileName: String): String {
    return testSuite.run {
      val testBody = testCases.joinToString(separator = System.lineSeparator(), postfix = System.lineSeparator())

      JavaClassBuilderHelper.generateCode(
        project,
        testFileName,
        testBody,
        imports,
        packageString,
        runWith,
        otherInfo,
        generatedTestsData,
      )
    }
  }

  override fun toStringSingleTestCaseWithoutExpectedException(
    testSuite: TestSuiteGeneratedByLLM,
    testCaseIndex: Int,
  ): String =
    testSuite.run {
      JavaClassBuilderHelper.generateCode(
        project,
        getClassWithTestCaseName(testCases[testCaseIndex].name),
        "${testCases[testCaseIndex].toStringWithoutExpectedException()}${System.lineSeparator()}",
        imports,
        packageString,
        runWith,
        otherInfo,
        generatedTestsData,
      )
    }

  override fun toStringWithoutExpectedException(testSuite: TestSuiteGeneratedByLLM, testFileName: String): String {
    var testBody = ""

    return testSuite.run {
      // Add each test (exclude expected exception)
      testCases.forEach { testCase -> testBody += "${testCase.toStringWithoutExpectedException()}${System.lineSeparator()}" }

      JavaClassBuilderHelper.generateCode(
        project,
        testFileName,
        testBody,
        imports,
        packageString,
        runWith,
        otherInfo,
        generatedTestsData,
      )
    }
  }

  override fun getPrintablePackageString(testSuite: TestSuiteGeneratedByLLM): String {
    return testSuite.run {
      when {
        packageString.isEmpty() || packageString.isBlank() -> ""
        else -> packageString
      }
    }
  }
}