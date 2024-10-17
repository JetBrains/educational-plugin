package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestBuildUtil
import com.jetbrains.edu.coursecreator.testGeneration.processing.TestCompilerFactory
import com.jetbrains.edu.coursecreator.testGeneration.processing.manager.TestAssemblerManager
import com.jetbrains.edu.coursecreator.testGeneration.processing.manager.TestPresenterManager
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.request.OpenAIRequestManager
import com.jetbrains.edu.coursecreator.testGeneration.request.PromptSizeReductionDefaultStrategy
import com.jetbrains.edu.coursecreator.testGeneration.request.PromptUtil.generatePrompt
import com.jetbrains.edu.coursecreator.testGeneration.util.*
import okio.Path.Companion.toPath
import org.apache.commons.io.FileUtils
import org.jetbrains.research.testspark.core.data.Report
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.LLMWithFeedbackCycle
import java.io.File
import java.net.URL
import java.util.*

private const val TEMP_DIRECTORY = "generatedTests"

class TestGenerator(private val project: Project) {

  fun generateTestSuite(
    psiHelper: PsiHelper,
    testFilename: String,
    testedFileInfo: TestedFileInfo,
    progressIndicator: TestProgressIndicator
  ): String {
    downloadTestLibs()

    val classesToTest = psiHelper.getAllClassesToTest(project, testedFileInfo.caret).toList()
    val testGenerationData = TestGenerationData()
    val initialPromptMessage = generatePrompt(project, psiHelper, 0, classesToTest)
    val report = Report()
    val packageName = psiHelper.getPackageName()
    val testPresenterManager = TestPresenterManager.getInstance(testedFileInfo.language)
    val testSuitePresenter = testPresenterManager.getTestSuitePresenter(project, testGenerationData)
    val testsPresenter = testPresenterManager.getTestsPresenter(testSuitePresenter, testFilename)
    val testAssembler =
      TestAssemblerManager.getInstance(testedFileInfo.language).getTestAssembler(project, progressIndicator, testGenerationData)
    val llmFeedbackCycle = LLMWithFeedbackCycle(
      report = report,
      initialPromptMessage = initialPromptMessage,
      promptSizeReductionStrategy = PromptSizeReductionDefaultStrategy(project, testGenerationData, psiHelper, classesToTest),
      testSuiteFilename = testFilename.withExtension(testedFileInfo.language),
      packageName = packageName,
      resultPath = getResultPath(),
      buildPath = getBuildPath(project),
      requestManager = OpenAIRequestManager(project),
      testsAssembler = testAssembler,
      testCompiler = TestCompilerFactory.createJavacTestCompiler(project, LLMSettingsState.DefaultLLMSettingsState.junitVersion),
      indicator = progressIndicator,
      requestsCountThreshold = SettingsArguments(project).requestsCountThreshold(),
      testsPresenter = testsPresenter,
      testStorage = TestsPersistentStorage
    )
    val testSuite = llmFeedbackCycle.run().generatedTestSuite ?: error("Failed to generate a test suite")
    return testSuitePresenter.toString(testSuite, testFilename)
  }

  private fun getResultPath(): String {
    val testResultDirectory = "${FileUtilRt.getTempDirectory()}${File.separatorChar}$TEMP_DIRECTORY${File.separatorChar}"
    val id = UUID.randomUUID().toString()
    return TestBuildUtil.getResultPath(id, testResultDirectory)
  }

  private fun downloadTestLibs() {
    LLMSettingsState.DefaultLLMSettingsState.junitVersion.libJar.forEach {
      FileUtils.copyURLToFile(URL(it.downloadUrl), LibraryPathsProvider.libPrefix.toPath().resolve(it.name).toFile(), 6000, 6000)
    }
  }

  private fun getBuildPath(project: Project): String {
    var buildPath = TestBuildUtil.getBuildPath(project)
    if (buildPath.isBlank()) {
      buildPath = ProjectRootManager.getInstance(project).contentRoots.first().path
    }
    return buildPath
  }

  private fun String.withExtension(language: Language) = plus(".java") // TODO
}
