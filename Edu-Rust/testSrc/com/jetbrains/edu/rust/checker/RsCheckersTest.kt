package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import org.rust.lang.RsLanguage

class RsCheckersTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      lesson {
        eduTask("OK") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/lib.rs", """
              pub fn foo() -> String {
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
      }
    }
  }

  fun `test rust course`() {
    CheckActionListener.expectedMessage { task ->
      when (task.name) {
        "OK" -> TestsOutputParser.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
