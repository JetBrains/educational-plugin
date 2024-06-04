package com.jetbrains.edu.kotlin.eduAssistant

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionParameter
import com.jetbrains.edu.learning.courseFormat.eduAssistant.FunctionSignature
import com.jetbrains.edu.learning.courseFormat.eduAssistant.SignatureSource
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.eduAssistant.context.buildAuthorSolutionContext
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.FunctionSignatureResolver
import com.jetbrains.edu.learning.eduAssistant.context.function.signatures.createPsiFileForSolution
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessorImpl
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.navigation.NavigationUtils
import org.junit.Test

class TaskProcessorTest : JdkCheckerTestBase() {

  @Test
  fun testFunctionsSetTextRepresentation() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val functionSet = taskProcessor.getFunctionsFromTask()
    val mainSignaturesList = listOf(
      "greet(name: String): String", "main(): Unit"
    )
    assertEquals(mainSignaturesList, functionSet)
    assertEquals(mainSignaturesList, task.taskFiles["src/main/kotlin/Main.kt"]?.functionSignatures?.toSet()?.map { it.toString() })
  }

  @Test
  fun testFunctionsSetTextRepresentationOnLargeCourse() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task2")
    val taskProcessor = TaskProcessorImpl(task)
    val functionSet = taskProcessor.getFunctionsFromTask()
    val mainSignaturesList = listOf(
      "greet(name: String): String", "main(): Unit"
    )
    val preDefinedSymbolsSignaturesList = listOf(
      "getPictureWidth(picture: String): Int",
      "add(a: Int, b: Int): Int",
      "sum(numbers: IntArray): Int",
      "applyOperation(a: Int, b: Int, operation: Function2<Int, Int, Int>): Int",
      "getPrinter(): Function0<Unit>",
      "nullableLength(s: String?): Int?"
    )
    val expected = (mainSignaturesList + preDefinedSymbolsSignaturesList)
    assertEquals(expected, functionSet)
    assertEquals(mainSignaturesList, task.taskFiles["src/main/kotlin/Main.kt"]?.functionSignatures?.toSet()?.map { it.toString() })
    assertEquals(
      preDefinedSymbolsSignaturesList,
      task.taskFiles["src/main/kotlin/Util.kt"]?.functionSignatures?.toSet()?.map { it.toString() })
    assertEquals(null, task.taskFiles["test/Tests.kt"]?.functionSignatures?.toSet()?.map { it.toString() })
  }

  @Test
  fun testFunctionsSetTextRepresentationFromTaskAndSolution() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson2", "task2")
    val taskProcessor = TaskProcessorImpl(task)
    val functionSet = taskProcessor.getFunctionsFromTask()
    val expected = listOf("main(): Unit")
    assertEquals(expected, functionSet)
    val expectedFromSolution = listOf("myPrint(): Unit", "main(): Unit")
    task.authorSolutionContext = task.buildAuthorSolutionContext() ?: error("Cannot build the author context")
    val actualFromSolution = task.authorSolutionContext?.functionSignatures?.map{ it.toString() }
    assertEquals(expectedFromSolution, actualFromSolution)
  }

  @Test
  fun testFailedTestsAndMessage() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson3", "task1")
    myCourse.configurator!!.courseBuilder.refreshProject(project, RefreshCause.PROJECT_CREATED)
    checkTask(task)
    val taskProcessor = TaskProcessorImpl(task)
    taskProcessor.getFailedTestName()?.let {
      // We have different behaviour on different IDEA versions
      assertTrue("Actual failed test name is $it", it in listOf("Test class Tests:testSolution", "Tests:testSolution"))
    }
    taskProcessor.getFailureMessage()?.let {
      // We have different behaviour on different IDEA versions
      assertTrue("Actual failed test message is $it", it in listOf("foo() should return 42", "Execution failed"))
    }
  }

  @Test
  fun testChangedFunctionsInSubmission() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task2")
    task.authorSolutionContext = task.buildAuthorSolutionContext()
    val taskProcessor = TaskProcessorImpl(task)
    val expected = """
        fun main() {
            val a = "AA"
            val b = stringTemplate
            println(a)
            println("Hello!")
        }
      """.trimIndent()
    NavigationUtils.navigateToTask(project, task)
    assertEquals(expected.reformatCode(project), taskProcessor.getSubmissionTextRepresentation()?.reformatCode(project))
    runWriteAction {
      val taskFile = task.taskFiles["src/main/kotlin/Main.kt"]
      val document = taskFile?.getDocument(project) ?: error("Document was not found")
      document.setText(
        """
          fun greet(name: String) = "Hello, \${'$'}\{name\}!"
          fun main() {
              val a = "AA"
              val b = stringTemplate
              println(a)
              println("Hello!")
          }
          fun newFunction() {
              println("Hello world!")
          }
        """.trimIndent()
      )
      FileDocumentManager.getInstance().saveDocument(document)
      PsiDocumentManager.getInstance(project).commitDocument(document)
      val expected2FirstFun =
        """
          fun main() {
              val a = "AA"
              val b = stringTemplate
              println(a)
              println("Hello!")
          }
        """.trimIndent()
      val expected2SecondFun =
        """
          fun newFunction() {
              println("Hello world!")
          }
        """.trimIndent()
      assertEquals(listOf(expected2FirstFun, expected2SecondFun).joinToString(separator = System.lineSeparator()).reformatCode(project), taskProcessor.getSubmissionTextRepresentation()?.reformatCode(project))
    }
  }

  @Test
  fun testStringsFromSolution() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task2")
    val context = task.buildAuthorSolutionContext() ?: error("Cannot build the author solution context")
    val strings = context.functionsToStringMap.values.flatten()
    val expected1 = listOf(
      "\"Hello, \\\$\\{name\\}!\"",
      "\"AA\"",
      "\"Hello!\"",
      "\"string\"",
    )
    val expected2 = listOf(
      "\"Printing...\""
    )
    assertEquals(expected1 + expected2, strings)
  }

  @Test
  fun testStringsFromUserSolution() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task2")
    val taskProcessor = TaskProcessorImpl(task)
    val strings = taskProcessor.getStringsFromTask()
    val expected1 = listOf(
      "\"Hello, \\\$\\{name\\}!\"",
      "\"AA\"",
      "\"Hello!\"",
      "\"string\"",
    )
    val expected2 = listOf(
      "\"Printing...\""
    )
    assertEquals(expected1 + expected2, strings)
  }

  @Test
  fun testTaskTextRepresentation() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson4", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val taskText = taskProcessor.getTaskTextRepresentation()
    val expected = """
It's time to write your first program in Kotlin! Task Change the output text into Hello! and run the program. fun main() {
    // Some code here
}
 Image Feedback Survey The survey is anonymous and should take no more than 5 minutes to complete.
    """.trimIndent()
    assertEquals(expected, taskText)
  }

  @Test
  fun testHintsTextRepresentation() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson4", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val hints = taskProcessor.getHintsTextRepresentation()
    val expected = listOf(
      """
        To run your program, you need to open the Main.kt file and click on the green triangle near the main function. Then, the output of the program will be shown in the console:
      """.trimIndent(), """
        If for some reason the survey link is unclickable, you can copy the full link here: https://surveys.jetbrains.com/link
      """.trimIndent()
    )
    assertEquals(expected, hints)
  }

  @Test
  fun testRecommendsImplementingShortFunction() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val state = project.eduState ?: error("State was not found")
    NavigationUtils.navigateToTask(project, task, state.task)
    val taskProcessor = TaskProcessorImpl(task)
    task.authorSolutionContext = task.buildAuthorSolutionContext()
    val generatedCode = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()
    assertEquals(
      generatedCode,
      taskProcessor.getShortFunctionFromSolutionIfRecommended(generatedCode, "greet").toString()
    )
  }

  @Test
  fun testGetFunctionBySignature() {
    val code = """
      fun getPictureWidth(picture: String) = picture.lines().maxOfOrNull { it.length } ?: 0
      fun add(a: Int, b: Int): Int {
          return a + b
      }
      fun getPrinter(): () -> Unit = { println("Printing...") }
      fun nullableLength(s: String?) = s?.length
    """.trimIndent()
    val psiFile = code.createPsiFileForSolution(project, language)
    var functionSignature = FunctionSignature(
      "getPictureWidth",
      listOf(FunctionParameter("picture", "String")),
      "String",
      SignatureSource.MODEL_SOLUTION
    )
    var expected = """
        fun getPictureWidth(picture: String) = picture.lines().maxOfOrNull { it.length } ?: 0
      """.trimIndent()
    assertEquals(expected, FunctionSignatureResolver.getFunctionBySignature(psiFile, functionSignature.name, language)?.text)
    functionSignature = FunctionSignature(
      "add",
      listOf(FunctionParameter("a", "Int"), FunctionParameter("b", "Int")),
      "Int",
      SignatureSource.MODEL_SOLUTION
    )
    expected = """
        fun add(a: Int, b: Int): Int {
            return a + b
        }
      """.trimIndent()
    assertEquals(expected, FunctionSignatureResolver.getFunctionBySignature(psiFile, functionSignature.name, language)?.text)
    functionSignature = FunctionSignature("getPrinter", emptyList(), "Function0<Unit>", SignatureSource.MODEL_SOLUTION)
    expected = """
        fun getPrinter(): () -> Unit = { println("Printing...") }
      """.trimIndent()
    assertEquals(expected, FunctionSignatureResolver.getFunctionBySignature(psiFile, functionSignature.name, language)?.text)
    functionSignature = FunctionSignature(
      "nullableLength",
      listOf(FunctionParameter("s", "String?")),
      "Int?",
      SignatureSource.MODEL_SOLUTION
    )
    expected = """
        fun nullableLength(s: String?) = s?.length
      """.trimIndent()
    assertEquals(expected, FunctionSignatureResolver.getFunctionBySignature(psiFile, functionSignature.name, language)?.text)
  }

  @Test
  fun testApplyCodeHint() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val codeHint = """
      fun main() {
          println("Hello!")
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
    val updatedUserCode = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
    assertEquals(updatedUserCode, taskProcessor.applyCodeHint(codeHint))
  }

  @Test
  fun testApplyCodeHintNewFunction() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    val codeHint = """
      fun newFunction() {
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
    val updatedUserCode = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
      }
      
      fun newFunction() {
          val firstUserAnswer = readlnOrNull()
      }
    """.trimIndent()
    assertEquals(updatedUserCode, taskProcessor.applyCodeHint(codeHint))
  }

  @Test
  fun testApplyCodeHintAlreadyImplementedFunction() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessorImpl(task)
    taskProcessor.getFunctionsFromTask()
    val codeHint = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent()
    val updatedUserCode = """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
      }
    """.trimIndent()
    assertEquals(updatedUserCode, taskProcessor.applyCodeHint(codeHint))
  }

  @Test
  fun testExtractRequiredFunctionsFromCodeHint() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson5", "task2")
    val state = project.eduState ?: error("State was not found")
    NavigationUtils.navigateToTask(project, task, state.task)
    val taskProcessor = TaskProcessorImpl(task)
    val codeHint = """
      package jetbrains.kotlin.course.first.date

      fun generateSecret() = "ABCDEFG"
      
      fun main() {
          println("Hello!")
      }
    """.trimIndent()
    val updatedCodeHint = """
      fun main() {
          println("Hello!")
      }
    """.trimIndent()
    assertEquals(updatedCodeHint, taskProcessor.extractRequiredFunctionsFromCodeHint(codeHint))
  }

  override fun createCourse(): Course = createKotlinCourse()
}
