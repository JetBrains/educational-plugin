package com.jetbrains.edu.scala.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import com.jetbrains.edu.learning.xmlEscaped
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaGradleCheckErrorTest : JdkCheckerTestBase() {
  override fun createCourse(): Course {
    return course(language = ScalaLanguage.INSTANCE, environment = "Gradle") {
      lesson {
        eduTask("compilationError") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }

        eduTask("testFail") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 42
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                fail("Message")
              }
            }
          """)
        }

        eduTask("comparisonTestFail") {
          scalaTaskFile("src/Task.scala", """
            class Task {
              def foo(): Int = 43
            }
          """)
          scalaTaskFile("test/TestSpec.scala", """
            import org.junit.runner.RunWith
            import org.scalatest.junit.JUnitRunner
            import org.scalatest.FunSuite
            
            @RunWith(classOf[JUnitRunner])
            class TestSpec extends FunSuite {
              test("Test") {
                assertResult(42) { new Task().foo() }
              }
            }
          """)
        }
        eduTask("gradleCustomRunConfiguration") {
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
                assertResult("Hello") { new Task().foo() }
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


        outputTask("outputTaskFail") {
          scalaTaskFile("src/Main.scala", """
            object Main {
              def main(args: Array[String]): Unit = {
                println("OK")
              }
            }
          """)
          taskFile("test/output.txt") {
            withText("OK!\n")
          }
        }
      }
    }
  }

  @Test
  fun `test scala errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals("${task.name} should be failed", CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "compilationError" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "testFail" -> equalTo("Message") to nullValue()
        "comparisonTestFail" ->
          equalTo("Expected 42, but got 43") to nullValue()
        "gradleCustomRunConfiguration" ->
          equalTo("Expected \"Hello[]\", but got \"Hello[!]\"".xmlEscaped) to nullValue()
        "outputTaskFail" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "OK!\n", actual = "OK\n"))
        else -> error("Unexpected task `${task.name}`")
      }
      assertThat("Checker message for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
