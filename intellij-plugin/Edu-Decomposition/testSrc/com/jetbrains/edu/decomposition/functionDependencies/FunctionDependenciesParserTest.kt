package com.jetbrains.edu.decomposition.functionDependencies

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.edu.kotlin.decomposition.psi.KtFunctionDependenciesParser

class FunctionDependenciesParserTest: BasePlatformTestCase() {

  private val parser = KtFunctionDependenciesParser()

  fun `test simple function dependency parsing`() {
    val kotlinFile = """
          fun `function A`() {
              dependsOn(::    `function B`, ::  `function C`)
          }
          
          fun `function B`() { }
          
          fun `function C`() { }
      """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "function A" to listOf("function B", "function C"),
      "function B" to emptyList(),
      "function C" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test multiple functions with dependencies`() {
    val kotlinFile = """
          fun `main function`() {
              dependsOn(::`helper one`, ::    `helper two`)
          }

          fun `helper one`() {
              dependsOn(  :: `inner helper`)
          }

          fun `helper two`() { }

          fun `inner helper`() { }
      """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "main function" to listOf("helper one", "helper two"),
      "helper one" to listOf("inner helper"),
      "helper two" to emptyList(),
      "inner helper" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test function without dependencies`() {
    val kotlinFile = """
          fun `standalone function`() { }
      """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "standalone function" to emptyList<String>()
    )

    assertEquals(expected, dependencies)
  }

  fun `test function with non-dependency calls`() {
    val kotlinFile = """
          fun `test function`() {
              println("This should not be parsed as a dependency")
              dependsOn(::`valid function`)
          }

          fun `valid function`() { }
      """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "test function" to listOf("valid function"),
      "valid function" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test function with dependsOn but no arguments`() {
    val kotlinFile = """
          fun `empty dependency function`() {
              dependsOn()
          }
      """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected: Map<String, List<String>> = mapOf(
      "empty dependency function" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test functions with incorrect syntax`() {
    val kotlinFile = """
      fun `incorrect syntax function`() {
        dependsOn(`function 1`, `function 2`)
      }
      
      fun `function 1`() {
        dependsOn(::    function 1, :: function 2) 
      }
      
      fun `function 2`() {
        dependsOn()
      }
      
    """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "incorrect syntax function" to emptyList<String>(),
      "function 1" to emptyList(),
      "function 2" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test functions with more than one dependsOn declaration`() {
    val kotlinFile = """
      fun `more than one dependsOn declaration`() {
        dependsOn(::`function 1`)
        dependsOn(::`function 2`)
      }
      
      fun `function 1`() {}
      
      fun `function 2`() {}
    """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "more than one dependsOn declaration" to listOf("function 1", "function 2"),
      "function 1" to emptyList(),
      "function 2" to emptyList()
    )

    assertEquals(expected, dependencies)
  }

  fun `test functions with dependency in functions with no spaces in the name`() {
    val kotlinFile = """
      fun functionA() {
        dependsOn(::      functionB, ::  functionC)
      }
      
      fun functionB() {
        dependsOn(::functionA, ::functionC)
      }
      
      fun functionC() {
        dependsOn(::functionA, ::functionB)
      }
    """.trimIndent()

    val psiFile = myFixture.configureByText("TestFile.kt", kotlinFile)
    val dependencies = parser.extractFunctionDependencies(listOf(psiFile))

    val expected = mapOf(
      "functionA" to listOf("functionB", "functionC"),
      "functionB" to listOf("functionA","functionC"),
      "functionC" to listOf("functionA", "functionB")
    )
    assertEquals(expected, dependencies)
  }
}