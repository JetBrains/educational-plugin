package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.testGeneration.PromptUtil.generatePrompt
import okio.Path.Companion.toPath
import org.apache.commons.io.FileUtils
import org.jetbrains.research.testspark.core.data.Report
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.LLMWithFeedbackCycle
import org.jetbrains.research.testspark.core.test.TestsPersistentStorage
import org.jetbrains.research.testspark.core.test.TestsPresenter
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import java.io.File
import java.net.URL
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class TestGenerator(private val project: Project) {

  fun generateFileTests(
    psiHelper: PsiHelper,
    testFilename: String,
    caret: Int,
    progressIndicator: TestProgressIndicator
  ): String {
    val classesToTest = psiHelper.getAllClassesToTest(project, caret)
    val testGenerationData = TestGenerationData()
    val initialPromptMessage = generatePrompt(project, psiHelper, 0, classesToTest)
    val report = Report()

    val testResultDirectory = "${FileUtilRt.getTempDirectory()}${File.separatorChar}testSparkResults${File.separatorChar}"
    val id = UUID.randomUUID().toString()

    LLMSettingsState.DefaultLLMSettingsState.junitVersion.libJar.forEach {
      FileUtils.copyURLToFile(URL(it.downloadUrl), LibraryPathsProvider.libPrefix.toPath().resolve(it.name).toFile(), 6000, 6000)
    }

    val resultPath = TestBuildUtil.getResultPath(id, testResultDirectory)

    val packageName = psiHelper.getPackageName()
    val testSuitePresenter = JUnitTestSuitePresenter(project, testGenerationData)
    val testFileName1 = testFilename.replace(".java", "")
    val testsPresenter = object : TestsPresenter {
      override fun representTestSuite(testSuite: TestSuiteGeneratedByLLM): String {
        return testSuitePresenter.toStringWithoutExpectedException(testSuite, testFileName1)
      }

      override fun representTestCase(testSuite: TestSuiteGeneratedByLLM, testCaseIndex: Int): String {
        return testSuitePresenter.toStringSingleTestCaseWithoutExpectedException(testSuite, testCaseIndex)
      }
    }
    var buildPath = TestBuildUtil.getBuildPath (project)
    if (buildPath.isBlank()) {
      buildPath = ProjectRootManager.getInstance(project).contentRoots.first().path
    }
    val llmFeedbackCycle = LLMWithFeedbackCycle(
      report = report,
      initialPromptMessage = initialPromptMessage,
      promptSizeReductionStrategy = PromptSizeReductionDefaultStrategy(project, testGenerationData, psiHelper, classesToTest),
      testSuiteFilename = testFilename,
      packageName = packageName,
      resultPath = resultPath,
      buildPath = buildPath,
      requestManager = OpenAIRequestManager(project),
      testsAssembler = JUnitTestsAssembler(project, progressIndicator, testGenerationData),
      testCompiler = TestCompilerFactory.createJavacTestCompiler(project, LLMSettingsState.DefaultLLMSettingsState.junitVersion),
      indicator = progressIndicator,
      requestsCountThreshold = 4, // TODO
      testsPresenter = testsPresenter,
      testStorage = object : TestsPersistentStorage {
        override fun saveGeneratedTest(packageString: String, code: String, resultPath: String, testFileName: String): String {
          var generatedTestPath = "$resultPath${File.separatorChar}"
          packageString.split(".").forEach { directory ->
            if (directory.isNotBlank()) generatedTestPath += "$directory${File.separatorChar}"
          }
          Path(generatedTestPath).createDirectories()
          val testFile = File("$generatedTestPath$testFileName")
          testFile.createNewFile()
          testFile.writeText(code)

          return "$generatedTestPath$testFileName"
        }

      }
    )
    val testSuite = llmFeedbackCycle.run().generatedTestSuite!!
    return testSuitePresenter.toString(testSuite, testFileName1)
  }

}