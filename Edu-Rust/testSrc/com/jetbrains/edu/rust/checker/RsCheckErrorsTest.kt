package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiff
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.nullValue
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import org.rust.lang.RsLanguage

class RsCheckErrorsTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      lesson {
        eduTask("EduCompilationFailed") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() ->  {
                  String::from("foo")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use task::foo;

              #[test]
              fn test() {
                  assert_eq!(String::from("foo"), foo());
              }
          """)
        }
        eduTask("EduTestFailed") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() -> String {
                  String::from("bar")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use task::foo;

              #[test]
              fn test() {
                  assert!(foo() == String::from("foo"), "Test error message");
              }
          """)
        }
        eduTask("EduComparisonTestFailed") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() -> String {
                  String::from("bar")
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use task::foo;

              #[test]
              fn test() {
                  assert_eq!(foo(), String::from("foo"));
              }
          """)
        }
        outputTask("OutputCompilationFailed") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  println("Hello, World");
              }
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
        }
        outputTask("OutputTestsFailed") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  println!("Hello, World");
              }
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
        }
      }
    }
  }

  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals(CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "EduCompilationFailed" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "EduTestFailed" -> containsString("Test error message") to nullValue()
        "EduComparisonTestFailed" -> any(String::class.java) to
          diff(CheckResultDiff(expected = "foo", actual = "bar"))
        "OutputCompilationFailed" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "OutputTestsFailed" ->
          equalTo("Expected output:\n<Hello, World!\n>\nActual output:\n<Hello, World\n>") to
            diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n"))
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
