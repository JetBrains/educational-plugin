package com.jetbrains.edu.rust.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.TestsOutputParser
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.rust.lang.RsLanguage

class RsCheckersTest : RsCheckersTestBase() {

  override fun createCourse(): Course {
    return course(language = RsLanguage) {
      lesson {
        eduTask("Edu") {
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
        outputTask("Output") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
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
        outputTask("OutputIgnoreWarnings") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  println!("Hello, World!");
              }
              fn foo() {}
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
        }
        outputTask("OutputWithSeveralBinaryTargets") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  println!("Hello, World!");
              }
          """)
          rustTaskFile("src/bin/foo.rs", """
              fn main() {
                  println!("Hello fom foo.rs!");
              }
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
        }
        outputTask("OutputWithDependencies") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"

            [dependencies]
            rand = "0.6.1"
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

  fun `test rust course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> TestsOutputParser.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
