package com.jetbrains.edu.coursecreator.testGeneration.util

import org.jetbrains.research.testspark.core.data.JUnitVersion

data class LLMSettingsState(
  var openAIName: String = DefaultLLMSettingsState.openAIName,
  var openAIToken: String = DefaultLLMSettingsState.openAIToken,
  var openAIModel: String = DefaultLLMSettingsState.openAIModel,
  var grazieName: String = DefaultLLMSettingsState.grazieName,
  var grazieToken: String = DefaultLLMSettingsState.grazieToken,
  var grazieModel: String = DefaultLLMSettingsState.grazieModel,
  var currentLLMPlatformName: String = DefaultLLMSettingsState.currentLLMPlatformName,
  var maxLLMRequest: Int = DefaultLLMSettingsState.maxLLMRequest,
  var requestsCountThreshold: Int = DefaultLLMSettingsState.requestsCountThreshold,
  var maxInputParamsDepth: Int = DefaultLLMSettingsState.maxInputParamsDepth,
  var maxPolyDepth: Int = DefaultLLMSettingsState.maxPolyDepth,
  var classPrompts: String = DefaultLLMSettingsState.classPrompts,
  var methodPrompts: String = DefaultLLMSettingsState.methodPrompts,
  var linePrompts: String = DefaultLLMSettingsState.linePrompts,
  var classPromptNames: String = DefaultLLMSettingsState.classPromptNames,
  var methodPromptNames: String = DefaultLLMSettingsState.methodPromptNames,
  var linePromptNames: String = DefaultLLMSettingsState.linePromptNames,
  var classCurrentDefaultPromptIndex: Int = DefaultLLMSettingsState.classCurrentDefaultPromptIndex,
  var methodCurrentDefaultPromptIndex: Int = DefaultLLMSettingsState.methodCurrentDefaultPromptIndex,
  var lineCurrentDefaultPromptIndex: Int = DefaultLLMSettingsState.lineCurrentDefaultPromptIndex,
  var defaultLLMRequests: String = DefaultLLMSettingsState.defaultLLMRequests,
  var junitVersionPriorityCheckBoxSelected: Boolean = DefaultLLMSettingsState.junitVersionPriorityCheckBoxSelected,
  var provideTestSamplesCheckBoxSelected: Boolean = DefaultLLMSettingsState.provideTestSamplesCheckBoxSelected,
  var llmSetupCheckBoxSelected: Boolean = DefaultLLMSettingsState.llmSetupCheckBoxSelected,
) {

  /**
   * Default values of SettingsLLMState.
   */
  object DefaultLLMSettingsState {
    val openAIName: String = "OpenAI"
    val openAIToken: String = ""
    val openAIModel: String = ""
    val grazieName: String = "AI Assistant JetBrains"
    val grazieToken: String = ""
    val grazieModel: String = ""
    var currentLLMPlatformName: String = "Grazie"
    val maxLLMRequest: Int = 3
    val requestsCountThreshold: Int = 3
    val maxInputParamsDepth: Int = 2
    val maxPolyDepth: Int = 2
    // TODO replace with bundle
    val classPrompts: String =
      "[\"Generate unit tests in ${'$'}LANGUAGE for ${'$'}NAME to achieve 100% line coverage for this class.\\nDont use @Before and @After test methods.\\nMake tests as atomic as possible.\\nAll tests should be for ${'$'}TESTING_PLATFORM.\\nIn case of mocking, use ${'$'}MOCKING_FRAMEWORK. But, do not use mocking for all tests.\\nName all methods according to the template - [MethodUnderTest][Scenario]Test, and use only English letters.\\nThe source code of class under test is as follows:\\n${'$'}CODE\\n${'$'}METHODS\\n${'$'}POLYMORPHISM\\n${'$'}TEST_SAMPLE\"]"
    val methodPrompts: String = ""
    val linePrompts: String = ""
    // TODO replace with bundle
    var classPromptNames = "[\"Class line coverage prompt\"]"
    var methodPromptNames = ""
    var linePromptNames = ""
    var classCurrentDefaultPromptIndex = 0
    var methodCurrentDefaultPromptIndex = 0
    var lineCurrentDefaultPromptIndex = 0
    // TODO replace with bundle
    val defaultLLMRequests: String =
      "[\"Add more comments to the test\",\"Reformat the test\",\"Improve variable names\",\"Improve assertions\",\"Increase the call sequences for a more complex scenario\"]"
    val junitVersion: JUnitVersion = JUnitVersion.JUnit4
    val junitVersionPriorityCheckBoxSelected: Boolean = false
    val provideTestSamplesCheckBoxSelected: Boolean = true
    val llmSetupCheckBoxSelected: Boolean = true
  }
}
