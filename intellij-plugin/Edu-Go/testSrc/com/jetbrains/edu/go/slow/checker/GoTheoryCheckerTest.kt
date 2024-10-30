package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class GoTheoryCheckerTest : GoCheckersTestBase() {
  override fun createCourse(): Course {
    return course(language = GoLanguage.INSTANCE) {
      lesson {
        theoryTask("Theory") {
          goTaskFile("main.go", """
            package main
            
            import (
              "fmt"
            )
            
            func main() {
              fmt.Println("Hello!")
            }
          """)
          taskFile("go.mod", """
            module theory
          """)
        }
        theoryTask("TheoryWithCustomRunConfiguration") {
          goTaskFile("main.go", """
            package main
            
            import (
              "fmt"
              "os"
            )
            
            func main() {
              fmt.Println(os.Getenv("EXAMPLE_ENV"))
            }
          """)
          taskFile("go.mod", """
            module theorywithcustomrunconfiguration
          """)
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="GoApplicationRunConfiguration" factoryName="Go Application">
                  <module name="Go Course" />
                  <working_directory value="${'$'}TASK_DIR${'$'}" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <kind value="PACKAGE" />
                  <filePath value="${'$'}TASK_DIR${'$'}/main.go" />
                  <package value="theorywithcustomrunconfiguration" />
                  <directory value="${'$'}PROJECT_DIR${'$'}" />
                  <method v="2" />
                </configuration>
              </component>              
            """)
          }
        }
      }
      frameworkLesson {
        theoryTask("FrameworkTheoryWithCustomRunConfiguration1") {
          goTaskFile("main.go", """
            package main
            
            import (
              "fmt"
              "os"
            )
            
            func main() {
              fmt.Println(os.Getenv("EXAMPLE_ENV"))
            }
          """)
          taskFile("go.mod", """
            module task
          """)
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun1" type="GoApplicationRunConfiguration" factoryName="Go Application">
                  <module name="Go Course" />
                  <working_directory value="${'$'}TASK_DIR${'$'}" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello from FrameworkTheory1!" />
                  </envs>
                  <kind value="PACKAGE" />
                  <package value="task" />
                  <directory value="${'$'}PROJECT_DIR${'$'}" />
                  <filePath value="${'$'}TASK_DIR${'$'}/main.go" />
                  <method v="2" />
                </configuration>
              </component>             
            """)
          }
        }
        theoryTask("FrameworkTheoryWithCustomRunConfiguration2") {
          goTaskFile("main.go", """
            package main
            
            import (
              "fmt"
              "os"
            )
            
            func main() {
              fmt.Println(os.Getenv("EXAMPLE_ENV"))
            }
          """)
          taskFile("go.mod", """
            module task
          """)
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun2" type="GoApplicationRunConfiguration" factoryName="Go Application">
                  <module name="Go Course" />
                  <working_directory value="${'$'}TASK_DIR${'$'}" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello from FrameworkTheory2!" />
                  </envs>
                  <kind value="PACKAGE" />
                  <package value="task" />
                  <directory value="${'$'}PROJECT_DIR${'$'}" />
                  <filePath value="${'$'}TASK_DIR${'$'}/main.go" />
                  <method v="2" />
                </configuration>
              </component>             
            """)
          }
        }
      }
    }
  }

  @Test
  fun `test go course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "Theory" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        "TheoryWithCustomRunConfiguration" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        "FrameworkTheoryWithCustomRunConfiguration1" -> equalTo(CheckStatus.Solved) to containsString("Hello from FrameworkTheory1!")
        "FrameworkTheoryWithCustomRunConfiguration2" -> equalTo(CheckStatus.Solved) to containsString("Hello from FrameworkTheory2!")
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat(checkResult.status, statusMatcher)
      assertThat(checkResult.message, messageMatcher)
    }
    doTest()
  }
}
