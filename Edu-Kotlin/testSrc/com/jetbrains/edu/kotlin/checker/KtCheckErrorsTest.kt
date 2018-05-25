package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtCheckErrorsTest : KtCheckersTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("kotlinCompilationError") {
        kotlinTaskFile("Task.kt", "fun foo(): Int = aaa")
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("javaCompilationError") {
        javaTaskFile("JavaClass.java", """
          public class JavaClass {
              public static int i = aaa;
          }
        """)
        kotlinTaskFile("Task.kt", """
          fun foo() = JavaClass.i
        """)
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("testFail") {
        kotlinTaskFile("Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
          }
        """)
      }
      eduTask("comparisonTestFail") {
        kotlinTaskFile("Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals(42, foo())
              }
          }
        """)
      }
      eduTask("comparisonTestWithMessageFail") {
        kotlinTaskFile("Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals("foo() should return 42", 42, foo())
              }
          }
        """)
      }
      eduTask("comparisonMultilineTestFail") {
        kotlinTaskFile("Task.kt", """
          fun foo(): String = "Hello\nWorld!"
        """)
        kotlinTestFile("Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test
              fun testSolution() {
                  Assert.assertEquals("Wrong Answer", "Hello,\nWorld!", foo())
              }
          }
        """)
      }
      outputTask("outputTaskFail") {
        kotlinTaskFile("Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        testFile("output.txt", "OK!")
      }
      outputTask("multilineOutputTaskFail") {
        kotlinTaskFile("Task.kt", """
          fun main(args: Array<String>) {
              println("Hello")
              println("World")
          }
        """)
        testFile("output.txt", "Hello,\nWorld!")
      }
    }
  }

  fun testErrors() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { task ->
      when (task.name) {
        "kotlinCompilationError", "javaCompilationError" -> CheckUtils.COMPILATION_FAILED_MESSAGE
        "testFail" -> "foo() should return 42"
        "comparisonTestFail" -> """
                                Expected: 42
                                Actual: 43
                                """
        "comparisonTestWithMessageFail" ->  """
                                            foo() should return 42
                                            Expected: 42
                                            Actual: 43
                                            """
        "comparisonMultilineTestFail" ->  """
                                          Wrong Answer
                                          Expected: Hello,
                                          World!
                                          Actual: Hello
                                          World!
                                          """
        "outputTaskFail" -> """
                            Expected output:
                            OK!
                            Actual output:
                            OK
                            """
        "multilineOutputTaskFail" ->  """
                                      Expected output:
                                      Hello,
                                      World!
                                      Actual output:
                                      Hello
                                      World
                                      """
        else -> null
      }?.trimIndent()
    }
    doTest()
  }

  fun testBrokenJdk() {
    UIUtil.dispatchAllInvocationEvents()

    val jdk = SdkConfigurationUtil.setupSdk(arrayOfNulls(0), myProject.baseDir, JavaSdk.getInstance(), true, null, "Broken JDK")
    runWriteAction {
      ProjectRootManager.getInstance(myProject).projectSdk = jdk
      ProjectJdkTable.getInstance().addJdk(jdk!!)
    }

    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { "${CheckUtils.FAILED_TO_CHECK_MESSAGE}. See idea.log for more details." }

    doTest()
  }
}
