package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.rust.lang.RsLanguage

class RsSingleWorkspaceCheckerTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      additionalFile("Cargo.toml",  """
        [workspace]
        members = [
            "lesson1/EduOk",
            "lesson1/EduErr",
            "lesson1/EduCompilationErr",
            "lesson1/Output"
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

  fun `test single workspace rust course`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      val (statusMatcher, messageMatcher) = when (task.name) {
        "EduOk" -> equalTo(CheckStatus.Solved) to equalTo(CheckUtils.CONGRATULATIONS)
        "EduErr" -> equalTo(CheckStatus.Failed) to containsString("assertion failed")
        "EduCompilationErr" -> equalTo(CheckStatus.Failed) to equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE)
        "Output" -> equalTo(CheckStatus.Solved) to equalTo(CheckUtils.CONGRATULATIONS)
        else -> error("Unexpected task name: ${task.name}")
      }
      Assert.assertThat("Checker status for ${task.name} doesn't match", checkResult.status, statusMatcher)
      Assert.assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
    }
  }
}
