package com.jetbrains.edu.scala.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaGradleCheckerTest : JdkCheckerTestBase() {
  override fun createCourse(): Course {
    return course(language = ScalaLanguage.INSTANCE, environment = "Gradle") {
      section {
        lesson {
          eduTask("EduTask in section") {
            scalaTaskFile("src/Task.scala", """
              class Task {
                def foo(): Int = 42
              }
            """)
            scalaTaskFile("test/TestSpec.scala", """
              import org.scalatest.FunSuite
  
              class TestSpec extends FunSuite {
                test("Test") {
                  assertResult(42) { new Task().foo() }
                }
              }
            """)
          }
        }
      }
      lesson {
        eduTask("EduTask") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 42
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.scalatest.FunSuite

            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }

        outputTask("OutputTask with main method") {
          scalaTaskFile("src/Main.scala", """
            object Main {
              def main(args: Array[String]): Unit = {
                println("OK")
              }
            }
          """)
          taskFile("test/output.txt") {
            withText("OK\n")
          }
        }

        outputTask("OutputTask with App") {
          scalaTaskFile("src/Main.scala", """
            object Main extends App {
              println("OK")
            }
          """)
          taskFile("test/output.txt") {
            withText("OK\n")
          }
        }

        outputTask("OutputTask with defined companion class") {
          scalaTaskFile("src/Main.scala", """
            class Main

            object Main {
              def main(args: Array[String]): Unit = {
                println("OK")
              }
            }
          """)
          taskFile("test/output.txt") {
            withText("OK\n")
          }
        }
      }
      frameworkLesson {
        eduTask("EduTask in framework lesson") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 42
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.scalatest.FunSuite

            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }
      }
    }
  }

  fun `test scala gradle course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }
}
