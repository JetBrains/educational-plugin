package com.jetbrains.edu.commandLine.validation

import org.junit.Test

class TeamCityValidationResultConsumerTest : ValidationResultConsumerTestBase() {

  override fun createResultConsumer(outputConsumer: ValidationOutputConsumer): ValidationResultConsumer {
    return TeamCityValidationResultConsumer(outputConsumer)
  }

  @Test
  fun `test output`() = doTest(sampleValidationResult(), """
    ##teamcity[testSuiteStarted name='courseSuite']
    ##teamcity[testSuiteStarted name='lessonSuite']
    ##teamcity[testSuiteStarted name='taskSuite']
    ##teamcity[testStarted name='case1']
    ##teamcity[testFinished name='case1']
    ##teamcity[testStarted name='case2']
    ##teamcity[testIgnored name='case2' message='ignored message']
    ##teamcity[testFinished name='case2']
    ##teamcity[testStarted name='case3']
    ##teamcity[testFailed name='case3' message='failed message' details='']
    ##teamcity[testFinished name='case3']
    ##teamcity[testStarted name='case4']
    ##teamcity[testFailed name='case4' message='failed message' details='failed details']
    ##teamcity[testFinished name='case4']
    ##teamcity[testStarted name='case5']
    ##teamcity[testFailed name='case5' message='failed message' details='failed details']
    ##teamcity[testFinished name='case5']
    ##teamcity[testSuiteFinished name='taskSuite']
    ##teamcity[testSuiteFinished name='lessonSuite']
    ##teamcity[testSuiteFinished name='courseSuite']
  """.trimIndent().trimEnd())
}
