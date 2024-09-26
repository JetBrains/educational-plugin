package com.jetbrains.edu.coursecreator.testGeneration.processing

import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM

interface TestSuitePresenter {
  fun toString(testSuite: TestSuiteGeneratedByLLM, testFileName: String): String

  /**
   * Returns the full text of the test suite (excluding the expected exception).
   *
   * @return the full text of the test suite (excluding the expected exception) as a string.
   */
  fun toStringSingleTestCaseWithoutExpectedException(
    testSuite: TestSuiteGeneratedByLLM,
    testCaseIndex: Int,
  ): String

  /**
   * Returns the full text of the test suite (excluding the expected exception).
   *
   * @return the full text of the test suite (excluding the expected exception) as a string.
   */
  fun toStringWithoutExpectedException(testSuite: TestSuiteGeneratedByLLM, testFileName: String): String

  /**
   * Returns a printable package string.
   *
   * If the package string is empty or consists of only whitespace characters, an empty string is returned.
   * Otherwise, the package string followed by a period is returned.
   *
   * @return The printable package string.
   */
  fun getPrintablePackageString(testSuite: TestSuiteGeneratedByLLM): String
}
