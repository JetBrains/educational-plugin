package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.jetbrains.edu.coursecreator.testGeneration.*
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.languageById
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

  private val testsPresenter = object : TestsPresenter {

    override fun representTestSuite(testSuite: TestSuiteGeneratedByLLM): String {
      return "TODO" // TODO
    }

    override fun representTestCase(testSuite: TestSuiteGeneratedByLLM, testCaseIndex: Int): String {
      return "TODO" // TODO
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


  // TODO Split into the smallest functions
  private fun generateTest(e: AnActionEvent) {
    val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
    val project = e.project ?: error("There are no project for this action") // TODO replace with the relevant behaviour
    val language = StudyTaskManager.getInstance(project).course?.languageById ?: error("There are no course or language instance") // TODO replace with the relevant behaviour
    val psiHelper = PsiHelper.getInstance(language)
    psiHelper.psiFile = psiFile // TODO refactor to the more convenient structure
    val caret = e.dataContext.getData(CommonDataKeys.CARET)?.caretModel?.primaryCaret!!.offset // TODO additional checking for caret
    val classesToTest = classesToTest(project, psiHelper, caret)


    val interestingPsiClasses =
      psiHelper.getInterestingPsiClassesWithQualifiedNames(project, classesToTest, 0) // TODO change polyDepth

    val interestingClasses = interestingPsiClasses.map { it.toClassRepresentation() }.toList()
    val polymorphismRelations =
      getPolymorphismRelationsWithQualifiedNames(project, interestingPsiClasses, classesToTest[0])
        .mapKeys { it.key.toClassRepresentation() }.mapValues { it.value.map { it.toClassRepresentation() } }
        .toMap()

    // cut - class under the test
    //classesToTest - cut + parents of the cut
    val context = PromptGenerationContext(
      cut = classesToTest[0].toClassRepresentation(),
      classesToTest = classesToTest.map { it.toClassRepresentation() }.toList(),
      polymorphismRelations = polymorphismRelations,
      promptConfiguration = PromptConfiguration(
        desiredLanguage = psiHelper.language.languageName,
        desiredTestingPlatform = "JUnit 5",
        desiredMockingFramework = "Mockito 5",
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

    val promptSizeReductionStrategy = object : PromptSizeReductionStrategy {
      override fun isReductionPossible(): Boolean = false // TODO add real logic

      override fun reduceSizeAndGeneratePrompt(): String {
        // TODO add real body
        return initialPromptMessage
      }
    }

    val report = Report()
    val testFilename = "GeneratedTest.java" // TODO hardcode solution
    val resultPath = "TODO" // TODO
    val buildPath = ProjectRootManager.getInstance(project).contentRoots.first().path

    val packageName = psiHelper.getPackageName()

    val llmFeedbackCycle = LLMWithFeedbackCycle(
      report = report,
      initialPromptMessage = initialPromptMessage,
      promptSizeReductionStrategy = promptSizeReductionStrategy,
      testSuiteFilename = testFilename,
      packageName = packageName,
      resultPath = resultPath,
      buildPath = buildPath,
      requestManager = OpenAIRequestManager(project),
      testsAssembler = JUnitTestsAssembler(project, progressIndicator, TestGenerationData()),
      testCompiler = TestCompilerFactory.createJavacTestCompiler(project, LLMSettingsState.DefaultLLMSettingsState.junitVersion),
      indicator = progressIndicator,
      requestsCountThreshold = 4, // TODO
      testsPresenter = testsPresenter,
      testStorage = object : TestsPersistentStorage {
        override fun saveGeneratedTest(packageString: String, code: String, resultPath: String, testFileName: String): String {
          return "TODO" // TODO
        }

      }
    )
    llmFeedbackCycle.run()
  }

}
