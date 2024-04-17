package com.jetbrains.edu.rust.slow.checker

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import org.junit.Test
import org.rust.lang.RsLanguage

class RsCodeforcesRunConfigurationTest : RsCheckersTestBase() {

  @Test
  fun `test application configuration`() {
    doCodeforcesTest("123456")
  }

  override fun createCourse(): Course = course(language = RsLanguage, courseProducer = ::CodeforcesCourse) {
    lesson(CodeforcesNames.CODEFORCES_PROBLEMS) {
      codeforcesTask("CodeforcesTask") {
        taskFile("Cargo.toml", """
            [package]
            name = "task"
            version = "0.1.0"
            edition = "2018"
        """)
        rustTaskFile("src/main.rs", """
            use std::io::{Read, stdin};
            
            fn main() {
                let mut buffer = String::new();
                stdin().read_line(&mut buffer);
                print!("{}", buffer);
            }
        """)
        taskFile("${CodeforcesNames.TEST_DATA_FOLDER}/1/input.txt", "123456")
        taskFile("${CodeforcesNames.TEST_DATA_FOLDER}/1/output.txt", "4")
      }
    }
  }
}
