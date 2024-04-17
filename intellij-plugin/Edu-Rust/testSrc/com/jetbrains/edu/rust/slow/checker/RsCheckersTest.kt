package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.checker.CheckActionListener
import com.jetbrains.edu.learning.checker.CheckUtils
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Test
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
        eduTask("EduWithIgnoredTest") {
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
              
              #[test]
              #[ignore]
              fn ignored_test() {
                  assert_eq!(1, 2);
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
        outputTask("OutputWithInput") {
          taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
          """)
          rustTaskFile("src/main.rs", """
              fn main() {
                  let text = std::io::stdin().lines().next().unwrap();
                  println!("{}", text.unwrap() + ", World!");
              }
          """)
          taskFile("tests/output.txt") {
            withText("Hello, World!\n")
          }
          taskFile("tests/input.txt") {
            withText("Hello")
          }
        }
      }
    }
  }

  @Test
  fun `test rust course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        else -> null
      }
    }
    doTest()
  }
}
