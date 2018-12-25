package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import org.rust.lang.RsLanguage

class RsCheckErrorsTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      lesson {
        eduTask("CompilationFailed") {
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
        eduTask("TestsFailed") {
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
                  assert_eq!(String::from("foo"), foo());
              }
          """)
        }
      }
    }
  }

  fun `test errors`() {
    CheckActionListener.shouldFail()
    CheckActionListener.expectedMessage { task ->
      when (task.name) {
        "CompilationFailed" -> CheckUtils.COMPILATION_FAILED_MESSAGE
        "TestsFailed" -> ""
        else -> null
      }
    }
    doTest()
  }
}
