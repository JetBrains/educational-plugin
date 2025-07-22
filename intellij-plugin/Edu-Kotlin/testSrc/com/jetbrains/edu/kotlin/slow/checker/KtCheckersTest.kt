package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtCheckersTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      eduTask("EduTask") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
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
      eduTask("EduTaskWithExclamationMark!") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
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
      eduTask("EduTaskWithIgnoredTest") {
        kotlinTaskFile("src/Task.kt", """
          fun foo() = 42
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Ignore          
          import org.junit.Test
          
          class Test {
              @Test
              fun testSolution() {
                  Assert.assertTrue("foo() should return 42", foo() == 42)
              }
              @Test
              @Ignore 
              fun ignoredTest() {
                  Assert.assertTrue("foo() should return 42", foo() == 43)
              }
          }
        """)
      }
      eduTask("EduTaskWithGradleCustomRunConfiguration") {
        @Suppress("RedundantNullableReturnType")
        kotlinTaskFile("src/Task.kt", """
          fun foo(): String? = System.getenv("EXAMPLE_ENV")
        """)
        kotlinTaskFile("test/Tests.kt", """
          import org.junit.Assert
          import org.junit.Test
          
          class Tests {
              @Test
              fun fail() {
                  Assert.fail()
              }
          
              @Test
              fun test() {
                  Assert.assertEquals("Hello!", foo())
              }
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleCheck.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleCheck" type="GradleRunConfiguration" factoryName="Gradle">
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
                      <option value="&quot;Tests.test&quot;" />
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
      outputTask("OutputTask") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskMainInsideTask") {
        kotlinTaskFile("src/Task.kt", """
          object Task {
            @JvmStatic
            fun main(args: Array<String>) {
              println("OK")  
            }
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTaskWithSeveralFiles") {
        kotlinTaskFile("src/utils.kt", """
          fun ok(): String = "OK"
        """)
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println(ok())
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
      outputTask("OutputTask:With;Special&Symbols?()") {
        kotlinTaskFile("src/Task.kt", """
          fun main(args: Array<String>) {
              println("OK")
          }
        """)
        taskFile("test/output.txt") {
          withText("OK\n")
        }
      }
    }
  }

  @Test
  fun `test kotlin course`() {
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
