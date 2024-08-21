package com.jetbrains.edu.coursecreator.actions

import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.project.LibraryData
import com.intellij.openapi.externalSystem.model.project.LibraryPathType
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependencies
import com.intellij.openapi.externalSystem.model.project.dependencies.ProjectDependenciesImpl
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.libraries.LibraryTable
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.PsiFile
import com.jetbrains.edu.coursecreator.testGeneration.*
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.languageById
import io.ktor.http.Url
import okio.Path.Companion.toPath
import org.apache.commons.io.FileUtils
import org.apache.velocity.runtime.resource.loader.JarResourceLoader
import org.jetbrains.research.testspark.core.data.JUnitVersion
import org.jetbrains.research.testspark.core.data.Report
import org.jetbrains.research.testspark.core.data.TestGenerationData
import org.jetbrains.research.testspark.core.generation.llm.LLMWithFeedbackCycle
import org.jetbrains.research.testspark.core.generation.llm.prompt.PromptGenerator
import org.jetbrains.research.testspark.core.generation.llm.prompt.PromptSizeReductionStrategy
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptConfiguration
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptGenerationContext
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptTemplates
import org.jetbrains.research.testspark.core.progress.CustomProgressIndicator
import org.jetbrains.research.testspark.core.test.TestsPersistentStorage
import org.jetbrains.research.testspark.core.test.TestsPresenter
import org.jetbrains.research.testspark.core.test.data.TestSuiteGeneratedByLLM
import org.jetbrains.research.testspark.core.utils.DataFilesUtil
import java.io.File
import java.net.URL
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories


open class GenerateTest : AnAction() {

  val progressIndicator = object : CustomProgressIndicator {
    override fun cancel() {
      // TODO
    }

    override fun getFraction(): Double {
      return 0.92
    }

    override fun getText(): String {
      return "TODO" // TODO
    }

    override fun isCanceled(): Boolean {
      return false
    }

    override fun isIndeterminate(): Boolean {
      return false
    }

    override fun setFraction(value: Double) {

    }

    override fun setIndeterminate(value: Boolean) {

    }

    override fun setText(text: String) {

    }

    override fun start() {

    }

    override fun stop() {

    }
  }


  override fun actionPerformed(e: AnActionEvent) {
    generateTest(e)
  }

  override fun update(e: AnActionEvent) {
    val project = e.project
    e.presentation.isEnabledAndVisible = project != null && isCourseJavaCreator(project)
  }

  private fun isCourseJavaCreator(project: Project): Boolean {
    val course = StudyTaskManager.getInstance(project).course ?: return false
    return (CourseMode.EDUCATOR == course.courseMode
            || CourseMode.EDUCATOR == EduUtilsKt.getCourseModeForNewlyCreatedProject(project))
           && course.languageId == "JAVA" // TODO
  }

  private fun classesToTest(project: Project, psiHelper: PsiHelper, caret: Int): List<PsiClassWrapper> {
    val classesToTest = mutableListOf<PsiClassWrapper>()
    ApplicationManager.getApplication().runReadAction(
      Computable {
        psiHelper.collectClassesToTest(project, classesToTest, caret) // TODO
      },
    )
    return classesToTest
  }


  fun getBuildPath(project: Project): String {
    var buildPath = ""

    for (module in ModuleManager.getInstance(project).modules) {
      val compilerOutputPath = CompilerModuleExtension.getInstance(module)?.compilerOutputPath

      compilerOutputPath?.let { buildPath += compilerOutputPath.path.plus(DataFilesUtil.classpathSeparator.toString()) }
      // Include extra libraries in classpath
      val librariesPaths = ModuleRootManager.getInstance(module).orderEntries().librariesOnly().pathsList.pathList
      for (lib in librariesPaths) {
        // exclude the invalid classpaths
        if (buildPath.contains(lib)) {
          continue
        }
        if (lib.endsWith(".zip")) {
          continue
        }

        // remove junit and hamcrest libraries, since we use our own libraries
        val pathArray = lib.split(File.separatorChar)
        val libFileName = pathArray[pathArray.size - 1]
        if (libFileName.startsWith("junit") ||
            libFileName.startsWith("hamcrest")
        ) {
          continue
        }

        buildPath += lib.plus(DataFilesUtil.classpathSeparator.toString())
      }
    }
    return buildPath
  }

  private fun generatePrompt(e: AnActionEvent, polyDepth: Int): String {
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: error("TODO")
    val project = e.project ?: error("There are no project for this action") // TODO replace with the relevant behaviour
    val language = StudyTaskManager.getInstance(project).course?.languageById
                   ?: error("There are no course or language instance") // TODO replace with the relevant behaviour
    val psiHelper = PsiHelper.getInstance(language)
    psiHelper.psiFile = psiFile // TODO refactor to the more convenient structure
    val caret = e.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret!!.offset // TODO additional checking for caret
    val classesToTest = classesToTest(project, psiHelper, caret)


    val interestingPsiClasses =
      psiHelper.getInterestingPsiClassesWithQualifiedNames(project, classesToTest, polyDepth) // TODO change polyDepth

    val interestingClasses = interestingPsiClasses.map { it.toClassRepresentation() }.toList()
    val polymorphismRelations =
      getPolymorphismRelationsWithQualifiedNames(project, interestingPsiClasses, classesToTest[0])
        .mapKeys { it.key.toClassRepresentation() }.mapValues { it.value.map { it.toClassRepresentation() } }
        .toMap()

    // cut - class under the test
    //classesToTest - cut + parents of the cut
    val context = PromptGenerationContext(
      cut = classesToTest.first().toClassRepresentation(),
      classesToTest = classesToTest.map { it.toClassRepresentation() }.toList(),
      polymorphismRelations = polymorphismRelations,
      promptConfiguration = PromptConfiguration(
        desiredLanguage = psiHelper.language.languageName,
        desiredTestingPlatform = "JUnit 4",
        desiredMockingFramework = "Without Mockito",
      )
    )

    // TODO replace to the bundle
    val promptTemplates = PromptTemplates(
      classPrompt = "[\"Generate unit tests in ${'$'}LANGUAGE for ${'$'}NAME to achieve 100% line coverage for this class.\\nDont use @Before and @After test methods.\\nMake tests as atomic as possible.\\nAll tests should be for ${'$'}TESTING_PLATFORM.\\nIn case of mocking, use ${'$'}MOCKING_FRAMEWORK. But, do not use mocking for all tests.\\nName all methods according to the template - [MethodUnderTest][Scenario]Test, and use only English letters.\\nThe source code of class under test is as follows:\\n${'$'}CODE\\n${'$'}METHODS\\n${'$'}POLYMORPHISM\\n${'$'}TEST_SAMPLE\"]",
      methodPrompt = "This is prompt", // TODO replace with the method prompt
      linePrompt = "This is prompt", // TODO replace with the method prompt
    )

    val promptGenerator = PromptGenerator(context, promptTemplates)
    val initialPromptMessage = promptGenerator.generatePromptForClass(interestingClasses, "") // TODO connect with real templates
    return initialPromptMessage
  }

  fun getResultPath(id: String, testResultDirectory: String): String {
    val testResultName = "test_gen_result_$id"

    return "$testResultDirectory$testResultName"
  }


  // TODO Split into the smallest functions
  private fun generateTest(e: AnActionEvent) {
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
    val project = e.project ?: error("There are no project for this action") // TODO replace with the relevant behaviour
    val libraries = LibraryTablesRegistrar.getInstance().getLibraryTable(project).libraries
    println("libraries size: ${libraries.size}")

    libraries.forEach {
      println("hello")
      val toFile = it.rootProvider.getFiles(OrderRootType.CLASSES)
        .find { it.extension == "jar" && it.nameWithoutExtension.contains("junit-4") }?.presentableUrl
        ?.toPath()?.toFile()
      toFile?.copyTo(LibraryPathsProvider.libPrefix.toPath().resolve(toFile.name).toFile(), true)
    }
    println("--------------")
    val language = StudyTaskManager.getInstance(project).course?.languageById
                   ?: error("There are no course or language instance") // TODO replace with the relevant behaviour
    println("--------------")
    val psiHelper = PsiHelper.getInstance(language)
    println("--------------")
    val testGenerationData = TestGenerationData()
    println("--------------")
    val initialPromptMessage = generatePrompt(e, 0)
    println("--------------")
    println(initialPromptMessage)
    println("--------------")

    val promptSizeReductionStrategy = object : PromptSizeReductionStrategy {
      override fun isReductionPossible(): Boolean = (SettingsArguments(project).maxPolyDepth(testGenerationData.polyDepthReducing) > 1) ||
                                                    (SettingsArguments(project).maxInputParamsDepth(testGenerationData.inputParamsDepthReducing) > 1)

      private fun reducePromptSize(): Boolean {
        if (SettingsArguments(project).maxPolyDepth(testGenerationData.polyDepthReducing) > 1) {
          testGenerationData.polyDepthReducing++
//          log.info("polymorphism depth is: ${SettingsArguments(project).maxPolyDepth(testGenerationData.polyDepthReducing)}")
//          showPromptReductionWarning(testGenerationData)
          return true
        }

        // reducing depth of input params
        if (SettingsArguments(project).maxInputParamsDepth(testGenerationData.inputParamsDepthReducing) > 1) {
          testGenerationData.inputParamsDepthReducing++
//          log.info("input params depth is: ${SettingsArguments(project).maxInputParamsDepth(testGenerationData.inputParamsDepthReducing)}")
//          showPromptReductionWarning(testGenerationData)
          return true
        }
        return false
      }

      override fun reduceSizeAndGeneratePrompt(): String {

        if (!isReductionPossible()) {
          throw IllegalStateException("Prompt size reduction is not possible yet requested")
        }

        val reductionSuccess = reducePromptSize()
        assert(reductionSuccess)

        return generatePrompt(e, testGenerationData.polyDepthReducing)
        // TODO add real body
//        return initialPromptMessage
      }
    }

    val report = Report()
    val testFilename = "GeneratedTest.java" // TODO hardcode solution

    val testResultDirectory = "${FileUtilRt.getTempDirectory()}${File.separatorChar}testSparkResults${File.separatorChar}"
    val id = UUID.randomUUID().toString()

    LLMSettingsState.DefaultLLMSettingsState.junitVersion.libJar.forEach { // download by sdk tools
      println(it.name)
      FileUtils.copyURLToFile(URL( it.downloadUrl), LibraryPathsProvider.libPrefix.toPath().resolve(it.name).toFile(),6000,6000)
    }

    val resultPath = getResultPath(id, testResultDirectory)
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
      promptSizeReductionStrategy = promptSizeReductionStrategy,
      testSuiteFilename = testFilename,
      packageName = packageName,
      resultPath = resultPath.toString().also { println("1234: $it") },
      buildPath = getBuildPath(project).also { println(it) },
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
    llmFeedbackCycle.run().also {
      println("================1=====================")
      println(it.generatedTestSuite)
      println("================2=====================")
      println(it.compilableTestCases)
      println("================3=====================")
      println(it.executionResult)
      println("================4=====================")
      println(testSuitePresenter.toString(it.generatedTestSuite!!))
    }

  }

}
