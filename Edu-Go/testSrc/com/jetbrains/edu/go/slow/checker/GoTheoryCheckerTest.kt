package com.jetbrains.edu.go.slow.checker

import com.goide.GoLanguage
import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import org.hamcrest.CoreMatchers
import org.junit.Assert

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
            taskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="GoApplicationRunConfiguration" factoryName="Go Application">
                  <module name="Go Course" />
                  <working_directory value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <kind value="PACKAGE" />
                  <filePath value="${'$'}PROJECT_DIR${'$'}/lesson1/TheoryWithCustomRunConfiguration/main.go" />
                  <package value="theorywithcustomrunconfiguration" />
                  <directory value="${'$'}PROJECT_DIR${'$'}" />
                  <method v="2" />
                </configuration>
              </component>              
            """)
          }
        }
      }
    }
  }

  fun `test go course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "Theory" -> CoreMatchers.equalTo(CheckStatus.Solved) to CoreMatchers.containsString("Hello!")
        "TheoryWithCustomRunConfiguration" -> CoreMatchers.equalTo(CheckStatus.Solved) to CoreMatchers.containsString("Hello!")
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat(checkResult.status, statusMatcher)
      Assert.assertThat(checkResult.message, messageMatcher)
    }
    doTest()
  }
}
