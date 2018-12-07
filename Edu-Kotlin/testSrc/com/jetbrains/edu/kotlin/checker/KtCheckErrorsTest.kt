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
        kotlinTaskFile("src/Task.kt", "fun foo(): Int = aaa")
        kotlinTaskFile("test/Tests.kt", """
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
        javaTaskFile("src/JavaClass.java", """
          public class JavaClass {
              public static int i = aaa;
          }
        """)
        kotlinTaskFile("src/Task.kt", """
          fun foo() = JavaClass.i
        """)
        kotlinTaskFile("test/Tests.kt", """
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
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
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
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
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
        kotlinTaskFile("src/Task.kt", """
          fun foo(): Int = 43
        """)
        kotlinTaskFile("test/Tests.kt", """
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
        kotlinTaskFile("src/Task.kt", """
          fun foo(): String = "Hello\nWorld!"
        """)
        kotlinTaskFile("test/Tests.kt", """
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
      eduTask("objectComparisonTestFail") {
        kotlinTaskFile("src/Task.kt", """
          data class Foo(val x: Int, val y: Int) {
              override fun toString(): String = "(${'$'}x, ${'$'}y)"
          }

          data class Bar(val x: Int, val y: Int) {
              override fun toString(): String = "(${'$'}x, ${'$'}y)"
          }
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test

          class Test {
              @Test fun testSolution() {
                  Assert.assertEquals(Foo(0, 0), Bar(0, 0))
              }
          }
        """)
      }
      outputTask("outputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt", "OK!")
      }
      outputTask("multilineOutputTaskFail") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("Hello")
              println("World")
          }
        """)
        taskFile("test/output.txt", "Hello,\nWorld!")
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
                                Expected:
                                <42>
                                Actual:
                                <43>
                                """
        "comparisonTestWithMessageFail" ->  """
                                            foo() should return 42
                                            Expected:
                                            <42>
                                            Actual:
                                            <43>
                                            """
        "comparisonMultilineTestFail" ->  """
                                          Wrong Answer
                                          Expected:
                                          <Hello[,]
                                          World!>
                                          Actual:
                                          <Hello[]
                                          World!>
                                          """
        "objectComparisonTestFail" -> """
                                      Expected:
                                      Foo<(0, 0)>
                                      Actual:
                                      Bar<(0, 0)>
                                      """
        "outputTaskFail" -> """
                            Expected output:
                            <OK!>
                            Actual output:
                            <OK>
                            """
        "multilineOutputTaskFail" ->  """
                                      Expected output:
                                      <Hello,
                                      World!>
                                      Actual output:
                                      <Hello
                                      World>
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
