package com.jetbrains.edu.kotlin.slow.checker

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.details.CheckDetailsView
import com.jetbrains.edu.learning.checker.details.MockCheckDetailsView
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

// Add Theory task with custom Kotlin configuration
// Currently, such test doesn't work because we haven't managed to make it compile and run in tests
// https://youtrack.jetbrains.com/issue/EDU-4262
class KtTheoryTaskCheckerTest : JdkCheckerTestBase() {

  override fun createCourse(): Course = course(language = KotlinLanguage.INSTANCE) {
    lesson {
      theoryTask("TheoryTask") {
        kotlinTaskFile("src/Main.kt", """
          fun main(args: Array<String>) {
              println("Hello!")
          }
        """)
      }
      theoryTask("TheoryWithGradleCustomRunConfiguration") {
        kotlinTaskFile("src/Main.kt", """
          fun main(args: Array<String>) {
              println(System.getenv("EXAMPLE_ENV"))
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleRun.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleRun" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="-PmainClass=MainKt" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":lesson1-TheoryWithGradleCustomRunConfiguration:run" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>
          """)
        }
      }
    }
    frameworkLesson {
      theoryTask("FrameworkTheoryWithCustomRunConfiguration1") {
        kotlinTaskFile("src/Main.kt", """
          fun main(args: Array<String>) {
              println(System.getenv("EXAMPLE_ENV"))
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleRun.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleRun1" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello from FrameworkTheory1!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="-PmainClass=MainKt" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":$TASK_GRADLE_PROJECT$:run" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>            
          """)
        }
      }
      theoryTask("FrameworkTheoryWithCustomRunConfiguration2") {
        kotlinTaskFile("src/Main.kt", """
          fun main(args: Array<String>) {
              println(System.getenv("EXAMPLE_ENV"))
          }
        """)
        dir("runConfigurations") {
          xmlTaskFile("CustomGradleRun.run.xml", $$"""
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomGradleRun2" type="GradleRunConfiguration" factoryName="Gradle">
                <ExternalSystemSettings>
                  <option name="env">
                    <map>
                      <entry key="EXAMPLE_ENV" value="Hello from FrameworkTheory2!" />
                    </map>
                  </option>
                  <option name="executionName" />
                  <option name="externalProjectPath" value="$PROJECT_DIR$" />
                  <option name="externalSystemIdString" value="GRADLE" />
                  <option name="scriptParameters" value="-PmainClass=MainKt" />
                  <option name="taskDescriptions">
                    <list />
                  </option>
                  <option name="taskNames">
                    <list>
                      <option value=":$TASK_GRADLE_PROJECT$:run" />
                    </list>
                  </option>
                  <option name="vmOptions" value="" />
                </ExternalSystemSettings>
                <ExternalSystemDebugServerProcess>true</ExternalSystemDebugServerProcess>
                <ExternalSystemReattachDebugProcess>true</ExternalSystemReattachDebugProcess>
                <DebugAllEnabled>false</DebugAllEnabled>
                <method v="2" />
              </configuration>
            </component>            
          """)
        }
      }
    }
  }

  @Test
  fun `test kotlin course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (message, statusMatcher, messageMatcher) = when (task.name) {
        "TheoryTask" -> {
          val message = (CheckDetailsView.getInstance(project) as MockCheckDetailsView).getMessage()
          Triple(message, equalTo(CheckStatus.Solved), containsString("Hello!"))
        }
        "TheoryWithGradleCustomRunConfiguration" -> Triple(
          checkResult.message,
          equalTo(CheckStatus.Solved),
          allOf(containsString("Hello!"), not(containsString("#educational_plugin")))
        )
        "FrameworkTheoryWithCustomRunConfiguration1" -> Triple(
          checkResult.message,
          equalTo(CheckStatus.Solved),
          allOf(containsString("Hello from FrameworkTheory1!"), not(containsString("#educational_plugin")))
        )
        "FrameworkTheoryWithCustomRunConfiguration2" -> Triple(
          checkResult.message,
          equalTo(CheckStatus.Solved),
          allOf(containsString("Hello from FrameworkTheory2!"), not(containsString("#educational_plugin")))
        )
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat(checkResult.status, statusMatcher)
      assertThat(message, messageMatcher)
    }
    doTest()
  }

}
