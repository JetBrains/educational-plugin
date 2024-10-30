package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.rust.lang.RsLanguage

class RsSingleWorkspaceCheckerTest : RsCheckersTestBase() {

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
        eduTask("EduOk") {
          taskFile("Cargo.toml", """
            [package]
            name = "edu_task_ok"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() -> String {
                  String::from("foo")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use edu_task_ok::foo;

              #[test]
              fn test() {
                  assert_eq!(String::from("foo"), foo());
              }
          """)
        }
        eduTask("EduErr") {
          taskFile("Cargo.toml", """
            [package]
            name = "edu_task_err"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() -> String {
                  String::from("foo")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use edu_task_err::foo;

              #[test]
              fn test() {
                  assert_eq!(String::from("bar"), foo());
              }
          """)
        }
        eduTask("EduCompilationErr") {
          taskFile("Cargo.toml", """
            [package]
            name = "edu_task_compilation_err"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() ->  {
                  String::from("foo")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use edu_task_compilation_err::foo;

              #[test]
              fn test() {
                  assert_eq!(String::from("bar"), foo());
              }
          """)
        }
        eduTask("EduWithCustomRunConfigurationOk") {
          taskFile("Cargo.toml", """
            [package]
            name = "edu_with_custom_run_configuration_ok"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              use std::env;

              pub fn hello() -> String {
                  return env::var("EXAMPLE_ENV").unwrap()
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use edu_with_custom_run_configuration_ok::hello;

              #[test]
              fn test() {
                  assert_eq!(hello(), "Hello!", "Error message");
              }

              #[test]
              fn fail() {
                  panic!("Error message")
              }
          """)
          xmlTaskFile("runConfigurations/CustomCheckOk.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomCheckOk" type="CargoCommandRunConfiguration" factoryName="Cargo Command">
                <option name="command" value="test --package edu_with_custom_run_configuration_ok --test tests test -- --exact" />
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
        eduTask("EduWithCustomRunConfigurationErr") {
          taskFile("Cargo.toml", """
            [package]
            name = "edu_with_custom_run_configuration_err"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              use std::env;

              pub fn hello() -> String {
                  return env::var("EXAMPLE_ENV").unwrap()
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use edu_with_custom_run_configuration_err::hello;

              #[test]
              fn test() {
                  assert_eq!(hello(), "Hello", "Error message");
              }

              #[test]
              fn fail() {
                  panic!("Error message")
              }
          """)
          xmlTaskFile("runConfigurations/CustomCheckErr.run.xml", """
            <component name="ProjectRunConfigurationManager">
              <configuration default="false" name="CustomCheckErr" type="CargoCommandRunConfiguration" factoryName="Cargo Command">
                <option name="command" value="test --package edu_with_custom_run_configuration_err --test tests test -- --exact" />
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
        outputTask("Output") {
          taskFile("Cargo.toml", """
            [package]
            name = "output_task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  println!("Hello, World!");
              }
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
        }
      }
    }
  }

  @Test
  fun `test single workspace rust course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "EduOk" -> equalTo(CheckStatus.Solved) to equalTo(CheckUtils.CONGRATULATIONS)
        "EduErr" -> equalTo(CheckStatus.Failed) to equalTo(EduCoreBundle.message("check.incorrect"))
        "EduCompilationErr" -> equalTo(CheckStatus.Failed) to equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE)
        "EduWithCustomRunConfigurationOk" -> equalTo(CheckStatus.Solved) to equalTo(CheckUtils.CONGRATULATIONS)
        "EduWithCustomRunConfigurationErr" -> equalTo(CheckStatus.Failed) to equalTo("Error message")
        "Output" -> equalTo(CheckStatus.Solved) to equalTo(CheckUtils.CONGRATULATIONS)
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker status for ${task.name} doesn't match", checkResult.status, statusMatcher)
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
    }
    doTest()
  }
}
