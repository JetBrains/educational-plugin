package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class CourseWithCustomContentPathTest : EduTestCase() {
  @Test
  fun `generate course with arbitrary path to lessons`() {
    courseWithFiles(courseMode = CourseMode.STUDENT, customPath = "some/path/to") {
      lesson {
        eduTask {
          taskFile("Fizz.kt")
        }
        eduTask {
          taskFile("Buzz.kt")
        }
      }
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt")
        }
      }
    }

    checkFileTree {
      dir("some/path/to") {
        dir("lesson1") {
          dir("task1") {
            file("Fizz.kt")
            file("task.md")
          }
          dir("task2") {
            file("Buzz.kt")
            file("task.md")
          }
        }
        dir("lesson2") {
          dir("task1") {
            file("FizzBuzz.kt")
            file("task.md")
          }
        }
      }
    }
  }

  @Test
  fun `generate course with arbitrary path to lessons with sections`() {
    courseWithFiles(courseMode = CourseMode.STUDENT, customPath = "some/path/to") {
      section {
        lesson {
          eduTask {
            taskFile("Fizz.kt")
          }
          eduTask {
            taskFile("Buzz.kt")
          }
        }
      }
      section {
        lesson {
          eduTask {
            taskFile("FizzBuzz.kt")
          }
        }
      }
    }

    checkFileTree {
      dir("some/path/to") {
        dir("section1") {
          dir("lesson1") {
            dir("task1") {
              file("Fizz.kt")
              file("task.md")
            }
            dir("task2") {
              file("Buzz.kt")
              file("task.md")
            }
          }
        }
        dir("section2") {
          dir("lesson1") {
            dir("task1") {
              file("FizzBuzz.kt")
              file("task.md")
            }
          }
        }
      }
    }
  }
}