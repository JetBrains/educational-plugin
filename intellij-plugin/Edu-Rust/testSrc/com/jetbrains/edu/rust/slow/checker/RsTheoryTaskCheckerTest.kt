package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Test
import org.rust.lang.RsLanguage

class RsTheoryTaskCheckerTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      additionalFile("Cargo.toml",  """
        [workspace]
        members = [
            "lesson1/*/",
        ]
        exclude = [
            "**/*.yaml"
        ]
      """)
      lesson("lesson1") {
        theoryTask("Theory") {
          rustTaskFile("src/main.rs", """
              fn main() {
                  println!("Hello!")
              }
          """)
          taskFile("Cargo.toml", """
            [package]
            name = "theory"
            version = "0.1.0"
            edition = "2018"
          """)
        }
        theoryTask("TheoryWithCustomRunConfiguration") {
          rustTaskFile("src/main.rs", """
            use std::env;
            fn main() {
                println!("{}", env::var("EXAMPLE_ENV").unwrap())
            }
          """)
          taskFile("Cargo.toml", """
            [package]
            name = "theory-with-custom-run-configuration"
            version = "0.1.0"
            edition = "2018"
          """)
          dir("runConfigurations") {
            xmlTaskFile("CustomRun.run.xml", """
              <component name="ProjectRunConfigurationManager">
                <configuration default="false" name="CustomRun" type="CargoCommandRunConfiguration" factoryName="Cargo Command">
                  <option name="command" value="run --package theory-with-custom-run-configuration --bin theory-with-custom-run-configuration" />
                  <option name="workingDirectory" value="file://${'$'}PROJECT_DIR${'$'}" />
                  <option name="channel" value="DEFAULT" />
                  <option name="requiredFeatures" value="true" />
                  <option name="allFeatures" value="false" />
                  <option name="emulateTerminal" value="false" />
                  <option name="backtrace" value="SHORT" />
                  <envs>
                    <env name="EXAMPLE_ENV" value="Hello!" />
                  </envs>
                  <option name="isRedirectInput" value="false" />
                  <option name="redirectInputPath" value="" />
                  <method v="2">
                    <option name="CARGO.BUILD_TASK_PROVIDER" enabled="true" />
                  </method>
                </configuration>
              </component>              
            """)
          }
        }
      }
    }
  }

  @Test
  fun `test rust course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "Theory" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        "TheoryWithCustomRunConfiguration" -> equalTo(CheckStatus.Solved) to containsString("Hello!")
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat(checkResult.status, statusMatcher)
      Assert.assertThat(checkResult.message, messageMatcher)
    }
    doTest()
  }
}
