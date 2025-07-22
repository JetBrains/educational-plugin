package com.jetbrains.edu.aiHints.kotlin

import com.intellij.openapi.application.runReadAction
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.aiHints.core.context.FunctionParameter
import com.jetbrains.edu.aiHints.core.context.FunctionSignature
import com.jetbrains.edu.aiHints.core.context.FunctionsToStrings
import com.jetbrains.edu.learning.EduTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtStringExtractorTest : EduTestCase() {
  override fun createCourse() {
    courseWithFiles(language = KotlinLanguage.INSTANCE) {
      lesson("lesson1") {
        eduTask("task1") {
          kotlinTaskFile(
            "task.kt", """
            const val stringTemplate = "string"

            fun greet(name: String) = "Hello, $\{name\}!"

            fun main() {
                val a = "AA"
                val b = stringTemplate
                println(a)
                println("Hello!")
            }
          """
          )
        }
      }
    }
  }

  override fun runInDispatchThread(): Boolean = false

  @Test
  fun `test getting map of function signatures to strings`() {
    val expectedResult = mapOf(
      FunctionSignature(
        "greet",
        listOf(FunctionParameter("name", "kotlin/String")),
        "kotlin/String",
        null,
        null,
      ) to listOf(""""Hello, \$\{name\}!""""),
      FunctionSignature(
        "main",
        listOf(),
        "kotlin/Unit",
        null,
        null
      ) to listOf(""""AA"""", """"Hello!"""", """"string"""")
    ).let(::FunctionsToStrings)

    val functionsToStringsMap = runReadAction {
      EduAIHintsProcessor.forCourse(getCourse())?.getStringsExtractor()?.getFunctionsToStringsMap(currentTaskPsiFile())
    }
    assertEquals(expectedResult, functionsToStringsMap)
  }

  private fun currentTaskPsiFile(): PsiFile {
    return PsiManager.getInstance(project).findFile(findFile("lesson1/task1/task.kt")) ?: error("Failed to find PSI File")
  }
}