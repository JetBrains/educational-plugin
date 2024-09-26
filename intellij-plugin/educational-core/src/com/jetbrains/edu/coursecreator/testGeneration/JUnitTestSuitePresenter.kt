package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.project.Project
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.getClassWithTestCaseName
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM

class JUnitTestSuitePresenter(
  private val project: Project,
  private val generatedTestsData: TestGenerationData,
) {
  /**
   * Returns a string representation of this object.
   *
   * The returned string includes the package name, imports, test class declaration, and test cases.
   *
   * If the package name is not blank, it is added to the string representation with the prefix "package ".
   *
   * Each import in the imports list is added to the string representation followed by a new line.
   *
   * The test class declaration "public class GeneratedTest{" is included in the string representation.
   *
   * Each test case in the testCases list is appended to the string representation.
   *
   * The test class closing bracket "}" is included in the string representation.
   *
   * @return A string representing the test file.
   */
  fun toString(testSuite: TestSuiteGeneratedByLLM, testFileName: String): String {
    var testBody = ""

    return testSuite.run {
      // Add each test
      testCases.forEach { testCase -> testBody += "$testCase\n" }

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

  /**
   * Returns the full text of the test suite (excluding the expected exception).
   *
   * @return the full text of the test suite (excluding the expected exception) as a string.
   */
  fun toStringSingleTestCaseWithoutExpectedException(
    testSuite: TestSuiteGeneratedByLLM,
    testCaseIndex: Int,
  ): String =
    testSuite.run {
      JavaClassBuilderHelper.generateCode(
        project,
        getClassWithTestCaseName(testCases[testCaseIndex].name),
        testCases[testCaseIndex].toStringWithoutExpectedException() + "\n",
        imports,
        packageString,
        runWith,
        otherInfo,
        generatedTestsData,
      )
    }

  /**
   * Returns the full text of the test suite (excluding the expected exception).
   *
   * @return the full text of the test suite (excluding the expected exception) as a string.
   */
  fun toStringWithoutExpectedException(testSuite: TestSuiteGeneratedByLLM,testFileName: String): String {
    var testBody = ""

    return testSuite.run {
      // Add each test (exclude expected exception)
      testCases.forEach { testCase -> testBody += "${testCase.toStringWithoutExpectedException()}\n" }

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

  /**
   * Returns a printable package string.
   *
   * If the package string is empty or consists of only whitespace characters, an empty string is returned.
   * Otherwise, the package string followed by a period is returned.
   *
   * @return The printable package string.
   */
  fun getPrintablePackageString(testSuite: TestSuiteGeneratedByLLM): String {
    return testSuite.run {
      when {
        packageString.isEmpty() || packageString.isBlank() -> ""
        else -> packageString
      }
    }
  }
}