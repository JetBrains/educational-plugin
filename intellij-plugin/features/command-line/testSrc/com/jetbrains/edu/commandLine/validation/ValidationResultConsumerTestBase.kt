package com.jetbrains.edu.commandLine.validation

import com.jetbrains.edu.coursecreator.validation.*
import com.jetbrains.edu.learning.EduTestCase
import kotlinx.coroutines.runBlocking

abstract class ValidationResultConsumerTestBase : EduTestCase() {

  protected abstract fun createResultConsumer(outputConsumer: ValidationOutputConsumer): ValidationResultConsumer

  protected fun doTest(rootNode: ValidationSuite, expectedOutput: String) {
    val outputConsumer = InMemoryValidationOutputConsumer()
    val resultConsumer = createResultConsumer(outputConsumer)
    resultConsumer.consume(rootNode)
    assertEquals(expectedOutput.trimIndent().trimEnd(), outputConsumer.output().trimEnd())
  }

  protected fun sampleValidationResult(): ValidationSuite {
    return runBlocking {
      withValidationTreeBuilder(ValidationResultNode.ROOT_NODE_NAME) {
        validationSuit("courseSuite") {
          validationSuit("lessonSuite") {
            validationSuit("taskSuite") {
              validationCase("case1", ValidationCaseResult.Success)
              validationCase("case2", ValidationCaseResult.Ignored("ignored message"))
              validationCase("case3", ValidationCaseResult.Failed("failed message"))
              validationCase("case4", ValidationCaseResult.Failed("failed message", "failed details"))
              validationCase("case5", ValidationCaseResult.Failed("failed message", "failed details", ValidationDiff("expected", "actual")))
            }
          }
        }
      }
    }
  }
}
