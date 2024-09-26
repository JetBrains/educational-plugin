package com.jetbrains.edu.coursecreator.testGeneration.request

import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiClassWrapper
import com.jetbrains.edu.coursecreator.testGeneration.psi.PsiHelper
import com.jetbrains.edu.coursecreator.testGeneration.util.getPolymorphismRelationsWithQualifiedNames
import com.jetbrains.edu.coursecreator.testGeneration.util.toClassRepresentation
import org.jetbrains.research.testspark.core.generation.llm.prompt.PromptGenerator
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptConfiguration
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptGenerationContext
import org.jetbrains.research.testspark.core.generation.llm.prompt.configuration.PromptTemplates

object PromptUtil {

  fun generatePrompt(
    project: Project,
    psiHelper: PsiHelper,
    polyDepth: Int,
    classesToTest: List<PsiClassWrapper>
  ): String {
    val interestingPsiClasses =
      psiHelper.getInterestingPsiClassesWithQualifiedNames(project, classesToTest, polyDepth)

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
    val initialPromptMessage = promptGenerator.generatePromptForClass(interestingClasses, "")
    return initialPromptMessage
  }
}
