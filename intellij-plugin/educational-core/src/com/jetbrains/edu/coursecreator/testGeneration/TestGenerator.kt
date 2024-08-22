package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtilRt
import com.jetbrains.edu.coursecreator.testGeneration.PromptUtil.generatePrompt
import okio.Path.Companion.toPath
import org.apache.commons.io.FileUtils
import org.jetbrains.research.testspark.core.data.Report
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.FeedbackResponse
import org.jetbrains.research.testspark.core.generation.llm.LLMWithFeedbackCycle
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsPersistentStorage
import org.jetbrains.research.testspark.core.test.TestsPresenter
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import java.io.File
import java.net.URL
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

class TestGenerator(private val project: Project) {

  val progressIndicator = ProgressIndicator()


  fun generateFileTests(
    psiHelper: PsiHelper,
    caret: Int
  ): FeedbackResponse { // TODO add possibility to generate uncompilable tests
    val classesToTest = psiHelper.getAllClassesToTest(project, caret)
    val testGenerationData = TestGenerationData()
    val initialPromptMessage = generatePrompt(project, psiHelper, 0, classesToTest)
    val interestingPsiClasses =
      psiHelper.getInterestingPsiClassesWithQualifiedNames(project, classesToTest, 0) // TODO change polyDepth
// ---------------------------------------------------
    val report = Report()
    val testFilename = "GeneratedTest.java" // TODO hardcode solution

    val testResultDirectory = "${FileUtilRt.getTempDirectory()}${File.separatorChar}testSparkResults${File.separatorChar}"
    val id = UUID.randomUUID().toString()

    LLMSettingsState.DefaultLLMSettingsState.junitVersion.libJar.forEach { // download by sdk tools
      println(it.name)
      FileUtils.copyURLToFile(URL( it.downloadUrl), LibraryPathsProvider.libPrefix.toPath().resolve(it.name).toFile(),6000,6000)
    }

    val resultPath = TestBuildUtil.getResultPath(id, testResultDirectory)
//    val buildPath = project.basePath!!.toPath().resolve("tests").also { it.toNioPath().toFile().mkdirs() }

    val packageName = psiHelper.getPackageName()
    val testSuitePresenter = JUnitTestSuitePresenter(project, testGenerationData)
    val testsPresenter = object : TestsPresenter {
      override fun representTestSuite(testSuite: TestSuiteGeneratedByLLM): String {
        return testSuitePresenter.toStringWithoutExpectedException(testSuite)
      }

      override fun representTestCase(testSuite: TestSuiteGeneratedByLLM, testCaseIndex: Int): String {
        return testSuitePresenter.toStringSingleTestCaseWithoutExpectedException(testSuite, testCaseIndex)
      }
    }


    val llmFeedbackCycle = LLMWithFeedbackCycle(
      report = report,
      initialPromptMessage = initialPromptMessage,
      promptSizeReductionStrategy = PromptSizeReductionDefaultStrategy(project, testGenerationData, psiHelper, classesToTest),
      testSuiteFilename = testFilename,
      packageName = packageName,
      resultPath = resultPath.toString().also { println("1234: $it") },
      buildPath = TestBuildUtil.getBuildPath(project).also { println(it) },
      requestManager = OpenAIRequestManager(project),
      testsAssembler = JUnitTestsAssembler(project, progressIndicator, testGenerationData),
      testCompiler = TestCompilerFactory.createJavacTestCompiler(project, LLMSettingsState.DefaultLLMSettingsState.junitVersion),
      indicator = progressIndicator,
      requestsCountThreshold = 4, // TODO
      testsPresenter = testsPresenter,
      testStorage = object : TestsPersistentStorage {
        override fun saveGeneratedTest(packageString: String, code: String, resultPath: String, testFileName: String): String {
          // Generate the final path for the generated tests
          var generatedTestPath = "$resultPath${File.separatorChar}"
          packageString.split(".").forEach { directory ->
            if (directory.isNotBlank()) generatedTestPath += "$directory${File.separatorChar}"
          }
          Path(generatedTestPath).createDirectories()

          // Save the generated test suite to the file
          val testFile = File("$generatedTestPath$testFileName")
          testFile.createNewFile()
//          log.info("Save test in file " + testFile.absolutePath)
          testFile.writeText(code)

          return "$generatedTestPath$testFileName"
        }

      }
    )
    return llmFeedbackCycle.run().also { println(it.generatedTestSuite) }
  }

}