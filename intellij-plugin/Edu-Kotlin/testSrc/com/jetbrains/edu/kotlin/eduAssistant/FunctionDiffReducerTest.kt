package com.jetbrains.edu.kotlin.eduAssistant

import com.intellij.openapi.ui.TestDialog
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.slow.checker.JdkCheckerFixture
import com.jetbrains.edu.kotlin.eduAssistant.courses.createKotlinCourse
import com.jetbrains.edu.learning.EduDocumentListener
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.EduCheckerFixture
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.withTestDialog
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.Test

@RunWith(Parameterized::class)
class FunctionDiffReducerTest(
  private val codeStr: String,
  private val codeHint: String,
  private val updatedCodeHint: String,
  private val functionName: String
) : HeavyPlatformTestCase() {

  companion object {
    @JvmStatic
    @Parameterized.Parameters
    fun data() = listOf(
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
      }
    """.trimIndent(), """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun newFunction() {
        val a = "AA"
        println(a)
        println("Hello!")
      }
      fun main() {
          println("Hello!")
      }
    """.trimIndent(),
        """
      fun newFunction() {
          TODO("Not yet implemented")
      }
    """.trimIndent(),
        "newFunction"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
      }
    """.trimIndent(), """
      fun greet(name: String, age: String) = "Hello, \${'$'}\{name\}\${'$'}\{age\}!"
      fun main() {
          println("Hello!")
      }
    """.trimIndent(),
        """
      fun greet(name: String, age: String) = "Hello, \${'$'}\{name\}!"
    """.trimIndent(), "greet"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          println("End")
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
          println("End")
      }
    """.trimIndent(),
        """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          println("End")
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
          println("Hello!")
      }
    """.trimIndent(),
        """
      fun main() {
          val firstUserAnswer: String = ""
          println("Hello!")
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          var firstUserAnswer: String = ""
          var secondUserAnswer: String = ""
          var thirdUserAnswer: String = ""
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
      }
    """.trimIndent(),
        """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          var secondUserAnswer: String = ""
          var thirdUserAnswer: String = ""
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          val fisrtUserAnswer: String = ""
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
      }
    """.trimIndent(),
        """
      fun main() {
          println("Hello!")
          val firstUserAnswer: String = ""
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          val firstUserAnswer: String = ""
          val secondUserAnswer: String = ""
          val thirdUserAnswer: String = ""
      }
    """.trimIndent(),
        """
      fun main() {
          val firstUserAnswer: String = ""
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        TODO("Not implemented")
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
          var complete: Boolean
      }
    """.trimIndent(),
        """
      fun main() {
          var complete: Boolean
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
          do {
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        } while (complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        } while (complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
            }
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (!isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (!isLost(complete, attempts, maxAttemptsCount)) {
            }
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            }
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            } else if (isWon(complete, attempts, maxAttemptsCount)) {
                println("Congratulations! You guessed it!")
                break
            }
        } while (!complete)
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean
        var attempts = 0
        do {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            printRoundResults(secret, guess)
            complete = isComplete(secret, guess)
            attempts++
            if (isLost(complete, attempts, maxAttemptsCount)) {
                println("Sorry, you lost! :( My word is \${'$'}secret")
                break
            }else if (isWon(complete, attempts, maxAttemptsCount)) {
                  }
        } while (!complete)
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val word = "Hello"
        if (word == "Hello") {
            println("Hello")
        } else if (word == "Hello!") {
            println("Hello!")
        }
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val word = "Hello"
        if (word == "Hello") {
            println("Hello")
        } else if (word == "Hello!") {
            println("Hello!")
        } else {
          println("Hello World!")
        }
      }
    """.trimIndent(),
        """
      fun main() {
        val word = "Hello"
        if (word == "Hello") {
            println("Hello")
        } else if (word == "Hello!") {
            println("Hello!")
        }else{
            println("Hello World!")
        }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var newUserWord = ""
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
            newUserWord += if (secret[i] == guess) {
                "secret[i]"
            } else {
                "currentUserWord[i * 2]"
            }
        }
      }
    """.trimIndent(),
        """
      fun main() {
        var newUserWord = ""
          for (i in secret.indices) {
        }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
        }
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
            newUserWord += if (secret[i] == guess) {
                "secret[i]"
            } else {
                "currentUserWord[i * 2]"
            }
        }
      }
    """.trimIndent(),
        """
      fun main() {
        var newUserWord = ""
        for (i in secret.indices) {
            newUserWord += if (secret[i] == guess) {
                "secret[i]"
            } else {
                "currentUserWord[i * 2]"
            }
        }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean = false
        while (!complete) {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
        }
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        var complete: Boolean = false
        while (!complete) {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
            complete = isComplete(secret, guess)
        }
      }
    """.trimIndent(),
        """
      fun main() {
        var complete: Boolean = false
        while (!complete) {
            println("Please input your guess. It should be of length \${'$'}wordLength.")
            val guess = safeReadLine()
        }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
            "borders" -> applyBordersFilter(trimmedPicture)
            "squared" -> applySquaredFilter(trimmedPicture)
            else -> error("Unexpected filter")
        }
      }
    """.trimIndent(),
        """
      fun main() {
        val trimmedPicture = trimPicture(picture)
          return when (filter) {
          }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
        when (filter1) {
        }
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
        when (filter) {
            "borders" -> applyBordersFilter(trimmedPicture)
            "squared" -> applySquaredFilter(trimmedPicture)
            else -> error("Unexpected filter")
        }
      }
    """.trimIndent(),
        """
      fun main() {
        val trimmedPicture = trimPicture(picture)
        when (filter) {
        }
      }
    """.trimIndent(), "main"),
      arrayOf("""
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
          "borders" -> applyBordersFilter(trimmedPicture)
          "squared" -> applySquaredFilter(trimmedPicture)
        }
      }
    """.trimIndent(),
        """
      fun greet(name: String) = "Hello, \${'$'}\{name\}!"
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
            "borders" -> applyBordersFilter(trimmedPicture)
            "squared" -> applySquaredFilter(trimmedPicture)
            else -> error("Unexpected filter")
        }
      }
    """.trimIndent(),
        """
      fun main() {
        val trimmedPicture = trimPicture(picture)
        return when (filter) {
          "borders" -> applyBordersFilter(trimmedPicture)
          "squared" -> applySquaredFilter(trimmedPicture)

            else -> error("Unexpected filter")
        }
      }
    """.trimIndent(), "main")
    )
  }

  @Test
  fun testReduceChangesInCodeHint() {
    assertEquals(
      updatedCodeHint.reformatCode(project),
      reduceChangesInCodeHint(codeStr, codeHint, functionName).reformatCode(project)
    )
  }

  private fun reduceChangesInCodeHint(codeStr: String, codeHint: String, functionName: String): String {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessor(task)
    val functionFromCode = taskProcessor.getFunctionPsiWithName(codeStr, functionName, project, language)
    val functionFromCodeHint = taskProcessor.getFunctionPsiWithName(codeHint, functionName, project, language)
    return taskProcessor.reduceChangesInCodeHint(functionFromCode, functionFromCodeHint, project, language)
  }

  private val checkerFixture: EduCheckerFixture<JdkProjectSettings> by lazy {
    JdkCheckerFixture()
  }

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    val skipTestReason = checkerFixture.getSkipTestReason()
    if (skipTestReason != null) {
      System.err.println("SKIP `$name`: $skipTestReason")
    }
    else {
      super.runTestRunnable(context)
    }
  }

  override fun setUpProject() {
    checkerFixture.setUp()
    if (checkerFixture.getSkipTestReason() == null) {
      val myCourse = createKotlinCourse()
      val settings = checkerFixture.projectSettings

      withTestDialog(TestDialog.NO) {
        val rootDir = tempDir.createVirtualDir()
        val generator = myCourse.configurator?.courseBuilder?.getCourseProjectGenerator(myCourse)
                        ?: error("Failed to get `CourseProjectGenerator`")
        myProject = generator.doCreateCourseProject(rootDir.path, settings)
                    ?: error("Cannot create project with name ${getTestName(true)}")
      }
    }
  }

  override fun setUp() {
    super.setUp()

    if (myProject != null) {
      EduDocumentListener.setGlobalListener(myProject, testRootDisposable)
    }

    CheckActionListener.registerListener(testRootDisposable)
    CheckActionListener.reset()
  }

  override fun tearDown() {
    try {
      checkerFixture.tearDown()
    } catch (_: Throwable) {
    } finally {
      super.tearDown()
    }
  }
}
