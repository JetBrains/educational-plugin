package com.jetbrains.edu.commandLine.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder
import com.jetbrains.edu.coursecreator.validation.ValidationCase
import com.jetbrains.edu.coursecreator.validation.ValidationCaseResult
import com.jetbrains.edu.coursecreator.validation.ValidationSuite

class TeamCityValidationResultConsumer(outputConsumer: ValidationOutputConsumer) : ValidationResultConsumer(outputConsumer) {

  override fun consume(rootNode: ValidationSuite) {
    // Intentionally don't emit events for root node, only for its children,
    // since it's a technical node and it's not necessary for TeamCity test hierarchy
    rootNode.processChildNodes()
  }

  private fun processValidationSuiteNode(suiteNode: ValidationSuite) {
    ServiceMessageBuilder.testSuiteStarted(suiteNode.name).consumeEvent()
    suiteNode.processChildNodes()
    ServiceMessageBuilder.testSuiteFinished(suiteNode.name).consumeEvent()
  }

  private fun ValidationSuite.processChildNodes() {
    for (node in children) {
      when (node) {
        is ValidationSuite -> processValidationSuiteNode(node)
        is ValidationCase -> processValidationCaseNode(node)
      }
    }
  }

  private fun processValidationCaseNode(caseNode: ValidationCase) {
    ServiceMessageBuilder.testStarted(caseNode.name).consumeEvent()
    caseNode.toResultEvent()?.consumeEvent()
    ServiceMessageBuilder.testFinished(caseNode.name).consumeEvent()
  }

  private fun ValidationCase.toResultEvent(): ServiceMessageBuilder? {
    return when (val validationResult = result) {
      ValidationCaseResult.Success -> null
      is ValidationCaseResult.Ignored -> ServiceMessageBuilder.testIgnored(name)
        .addAttribute(MESSAGE, validationResult.message)
      is ValidationCaseResult.Failed -> ServiceMessageBuilder.testFailed(name)
        .addAttribute(MESSAGE, validationResult.message)
        .addAttribute(DETAILS, validationResult.details.orEmpty())
    }
  }

  private fun ServiceMessageBuilder.consumeEvent() {
    outputConsumer.consume(toString())
  }

  companion object {
    private const val MESSAGE = "message"
    private const val DETAILS = "details"
  }
}
