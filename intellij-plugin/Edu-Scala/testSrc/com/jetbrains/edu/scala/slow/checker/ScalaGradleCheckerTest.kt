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
import org.junit.Test

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

        eduTask("EduTaskWithGradleCustomRunConfiguration") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): String = {
                System.getenv("EXAMPLE_ENV")
              }
            }
          """)
          scalaTaskFile("test/Test.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class Test extends FunSuite {
              test("hello") {
                assertResult("Hello!") { new Task().foo() }
              }
            }
        """)
          dir("runConfigurations") {
            xmlTaskFile("CustomGradleCheck.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration name="CustomGradleCheck" type="GradleRunConfiguration" factoryName="Gradle" temporary="true">
                  <ExternalSystemSettings>
                    <option name="env">
                      <map>
                        <entry key="EXAMPLE_ENV" value="Hello!" />
                      </map>
                    </option>
                    <option name="executionName" />
                    <option name="externalProjectPath" value="${'$'}PROJECT_DIR$" />
                    <option name="externalSystemIdString" value="GRADLE" />
                    <option name="scriptParameters" value="" />
                    <option name="taskDescriptions">
                      <list />
                    </option>
                    <option name="taskNames">
                      <list>
                        <option value=":${'$'}TASK_GRADLE_PROJECT$:test" />
                        <option value="--tests" />
                        <option value="&quot;Test&quot;" />
                      </list>
                    </option>
                    <option name="vmOptions" value="" />
                  </ExternalSystemSettings>
                  <ExternalSystemDebugServerProcess>false</ExternalSystemDebugServerProcess>
                  <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                  <DebugAllEnabled>false</DebugAllEnabled>
                  <method v="2" />
                </configuration>
              </component>              
            """)
          }
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

        outputTask("OutputTask with input.txt") {
          scalaTaskFile("src/Main.scala", """
            object Main {
                def main(args: Array[String]): Unit = {
                    val text = scala.io.StdIn.readLine()
                    println(text + ", World!")
                }
            }
          """)
          taskFile("test/output.txt") {
            withText("Hello, World!")
          }
          taskFile("test/input.txt") {
            withText("Hello")
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

  @Test
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
