package com.jetbrains.edu.coursecreator.testGeneration

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.HttpRequests
import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsAssembler
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import org.jetbrains.research.testspark.core.test.parsers.TestSuiteParser
import org.jetbrains.research.testspark.core.test.parsers.java.JUnitTestSuiteParser

class JUnitTestsAssembler(
  val project: Project,
  val indicator: CustomProgressIndicator,
  val generationData: TestGenerationData,
) : TestsAssembler() {

  private val log: Logger = Logger.getInstance(this.javaClass)

  private var lastTestCount = 0

  /**
   * Receives a response text and updates the progress bar accordingly.
   *
   * @param text part of the LLM response
   */
  override fun consume(text: String) {
    if (text.isEmpty()) return

    // Collect the response and update the progress bar
    super.consume(text)
    updateProgressBar()
  }

  /**
   * Receives a response text and updates the progress bar accordingly.
   *
   * @param httpRequest the httpRequest sent to OpenAI
   */
  fun consume(httpRequest: HttpRequests.Request) {
    while (true) {
      if (indicator.isProcessCanceled()) return

      Thread.sleep(50L)
      var text = httpRequest.reader.readLine()

      if (text.isEmpty()) continue

      text = text.removePrefix("data: ")

      val choices =
        Gson().fromJson(
          JsonParser.parseString(text)
            .asJsonObject["choices"]
            .asJsonArray[0].asJsonObject,
          OpenAIRequestManager.OpenAIChoice::class.java,
        )

      if (choices.finishedReason == "stop") break

      // Collect the response and update the progress bar
      super.consume(choices.delta.content)
      updateProgressBar()
    }

    log.debug(super.getContent())
  }

  private fun updateProgressBar() {
    val generatedTestsCount = super.getContent().split("@Test").size - 1

    if (lastTestCount != generatedTestsCount) {
      indicator.setText("Generating Test #" + generatedTestsCount)
      lastTestCount = generatedTestsCount
    }
  }

  override fun assembleTestSuite(packageName: String): TestSuiteGeneratedByLLM? {
    val junitVersion = LLMSettingsState.DefaultLLMSettingsState.junitVersion

    val parser = createTestSuiteParser(packageName, junitVersion)
    val testSuite: TestSuiteGeneratedByLLM? = parser.parseTestSuite(super.getContent())

    // save RunWith
    if (testSuite?.runWith?.isNotBlank() == true) {
      generationData.runWith = testSuite.runWith
      generationData.importsCode.add(junitVersion.runWithAnnotationMeta.import)
    }
    else {
      generationData.runWith = "" // TODO
      generationData.importsCode.remove(junitVersion.runWithAnnotationMeta.import)
    }

    // save annotations and pre-set methods
    generationData.otherInfo = testSuite?.otherInfo ?: ""

    // logging generated test cases if any
    testSuite?.testCases?.forEach { testCase -> log.info("Generated test case: $testCase") }
    return testSuite
  }

  private fun createTestSuiteParser(packageName: String, jUnitVersion: JUnitVersion): TestSuiteParser {
    return JUnitTestSuiteParser(packageName, jUnitVersion)
  }

  private fun CustomProgressIndicator.isProcessCanceled(): Boolean {
    if (isCanceled()) {
      stop()
      return true
    }
    return false
  }
}
