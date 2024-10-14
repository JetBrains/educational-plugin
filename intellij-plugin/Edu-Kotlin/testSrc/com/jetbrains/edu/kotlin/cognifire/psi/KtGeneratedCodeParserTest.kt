package com.jetbrains.edu.kotlin.cognifire.psi

import com.jetbrains.edu.cognifire.GeneratedCodeParser
import com.jetbrains.edu.cognifire.models.FunctionSignature
import com.jetbrains.edu.learning.EduTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtGeneratedCodeParserTest : EduTestCase() {

  @Test
  fun `test hasErrors without todo blocks`() {
    val generatedCode = """
      val picture = getPicture()
      val filter = chooseFilter()
      println("The transformed picture:")
      println(applyFilter(picture, filter))
    """.trimIndent()
    assertFalse(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with todo block`() {
    val generatedCode = """
      if (secret == deleteSeparator(currentGuess)) {
          TODO("Specify what to do if the comparison is true")
      }
    """.trimIndent()

    assertTrue(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with empty todo`() {
    val generatedCode = """
      val wordLength = 4
      val maxAttemptsCount = 3
      val secretExample = "ACEB"
      println(getGameRules(wordLength, maxAttemptsCount, secretExample))
      playGame(generateSecret(), wordLength, maxAttemptsCount)
      TODO()
    """.trimIndent()
    assertTrue(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  @Test
  fun `test hasErrors with todo messages related to return value`() {
    val generatedCode = """
      val wordLength = 4
      val maxAttemptsCount = 3
      val secretExample = "ACEB"
      println(getGameRules(wordLength, maxAttemptsCount, secretExample))
      playGame(generateSecret(), wordLength, maxAttemptsCount)
      TODO("Specify the return value or further operations")
    """.trimIndent()
    assertFalse(GeneratedCodeParser.hasErrors(project, generatedCode, mainFunctionSignature, language))
  }

  companion object {
    private val language = KotlinLanguage.INSTANCE
    private val mainFunctionSignature = FunctionSignature(
      name = "main",
      functionParameters = emptyList(),
      returnType = "Unit",
    )
  }
}
