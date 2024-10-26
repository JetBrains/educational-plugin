package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckResultDiffMatcher.Companion.diff
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckResultDiff
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.nullValue
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
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
                  assert!(foo() == String::from("foo"));
              }
          """)
        }
        eduTask("EduTestFailedWithMessage") {
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
        eduTask("EduTestFailedWithMultilineMessage") {
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
                  assert_ne!(foo(), String::from("bar"), "Test\nerror\nmessage");
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
              pub fn foo() -> i32 {
                  123
              }
          """)
          rustTaskFile("tests/tests.rs", """
              use task::foo;

              #[test]
              fn test() {
                  assert_eq!(foo(), 12);
              }
          """)
        }
        eduTask("EduComparisonTestFailedWithMessage") {
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
                  assert_eq!(foo(), String::from("foo"), "Test error message");
              }
          """)
        }
        eduTask("DoNotEscapeMessageInFailedTest") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs")
          rustTaskFile("tests/tests.rs", """
              #[test]
              fn test() {
                  assert!(false, "<br>");
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

  @Test
  fun `test errors`() {
    CheckActionListener.setCheckResultVerifier { task, checkResult ->
      assertEquals(CheckStatus.Failed, checkResult.status)
      val (messageMatcher, diffMatcher) = when (task.name) {
        "EduCompilationFailed" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "EduTestFailed" -> equalTo("""assertion failed: foo() == String::from(\"foo\")""") to nullValue()
        "EduTestFailedWithMessage" -> equalTo("Test error message") to nullValue()
        "EduTestFailedWithMultilineMessage" -> equalTo("Test\nerror\nmessage") to nullValue()
        "EduComparisonTestFailed" -> equalTo(EduCoreBundle.message("check.incorrect")) to
          diff(CheckResultDiff(expected = "12", actual = "123", title = "Comparison Failure (test)"))
        "EduComparisonTestFailedWithMessage" -> equalTo("Test error message") to
          diff(CheckResultDiff(expected = "foo", actual = "bar", title = "Comparison Failure (test)"))
        "DoNotEscapeMessageInFailedTest" -> equalTo("<br>") to nullValue()
        "OutputCompilationFailed" -> equalTo(CheckUtils.COMPILATION_FAILED_MESSAGE) to nullValue()
        "OutputTestsFailed" ->
          equalTo(EduCoreBundle.message("check.incorrect")) to
            diff(CheckResultDiff(expected = "Hello, World!\n", actual = "Hello, World\n"))
        else -> error("Unexpected task name: ${task.name}")
      }
      assertThat("Checker output for ${task.name} doesn't match", checkResult.message, messageMatcher)
      assertThat("Checker diff for ${task.name} doesn't match", checkResult.diff, diffMatcher)
    }
    doTest()
  }
}
