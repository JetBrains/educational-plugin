package com.jetbrains.edu.coursecreator.testGeneration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.concurrency.awaitPromise
import com.intellij.openapi.concurrency.waitForPromise
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.platform.diagnostic.telemetry.EDT
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.task.ProjectTaskManager
import com.intellij.util.concurrency.Semaphore
import com.jetbrains.edu.coursecreator.testGeneration.PromptUtil.generatePrompt
import kotlinx.coroutines.runBlocking
import okhttp3.internal.wait
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
import kotlin.time.Duration

class TestGenerator(private val project: Project) {

  val progressIndicator = ProgressIndicator()


  fun generateFileTests(
    psiHelper: PsiHelper,
    testFilename: String,
    caret: Int
  ): String { // TODO add possibility to generate uncompilable tests
    val classesToTest = psiHelper.getAllClassesToTest(project, caret)
    val testGenerationData = TestGenerationData()
    val initialPromptMessage = generatePrompt(project, psiHelper, 0, classesToTest)
    val interestingPsiClasses =
      psiHelper.getInterestingPsiClassesWithQualifiedNames(project, classesToTest, 0) // TODO change polyDepth
// ---------------------------------------------------
    val report = Report()
//    val testFilename = "GeneratedTest.java" // TODO hardcode solution

    val testResultDirectory = "${FileUtilRt.getTempDirectory()}${File.separatorChar}testSparkResults${File.separatorChar}"
    val id = UUID.randomUUID().toString()

    LLMSettingsState.DefaultLLMSettingsState.junitVersion.libJar.forEach { // download by sdk tools
      println(it.name)
      FileUtils.copyURLToFile(URL(it.downloadUrl), LibraryPathsProvider.libPrefix.toPath().resolve(it.name).toFile(), 6000, 6000)
    }

    val resultPath = TestBuildUtil.getResultPath(id, testResultDirectory)
//    val buildPath = project.basePath!!.toPath().resolve("tests").also { it.toNioPath().toFile().mkdirs() }

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

    ProjectRootManager.getInstance(project).contentRoots.first().path.also {
      println("321 $it")
    }

//    val promise = ProjectTaskManager.getInstance(project).buildAllModules()
//    promise.then {
//      println("Hello21")
//    }
//    println("Hello12")
//    val finished = Semaphore()
//    finished.down()
//    promise.onSuccess {
//      if (it.isAborted || it.hasErrors()) {
//        println("oh oh")
//      }
//      finished.up()
//    }
//    promise.onError {
//      println("oh oh1")
//      finished.up()
//    }
//    finished.waitFor()
    var buildPath = TestBuildUtil.getBuildPath (project)
    println("432 $buildPath")
    if (buildPath.isBlank()) {
      buildPath = ProjectRootManager.getInstance(project).contentRoots.first().path
    }
    val llmFeedbackCycle = LLMWithFeedbackCycle(
      report = report,
      initialPromptMessage = initialPromptMessage,
      promptSizeReductionStrategy = PromptSizeReductionDefaultStrategy(project, testGenerationData, psiHelper, classesToTest),
      testSuiteFilename = testFilename,
      packageName = packageName,
      resultPath = resultPath.toString().also { println("1234: $it") },
      buildPath = buildPath.also { println(it) },
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
    val testSuite = llmFeedbackCycle.run().generatedTestSuite!!
    println(testSuite)
    println(testFileName1)
    return testSuitePresenter.toString(testSuite, testFileName1)
  }

}