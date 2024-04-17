package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.junit.Test

class PlainTextCourseGeneratorTest : EduTestCase() {

  @Test
  fun `test course structure creation`() {
    courseWithFiles {
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

  @Test
  fun `test course with framework lesson structure creation`() {
    courseWithFiles {
      frameworkLesson {
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
      dir("lesson1") {
        dir("task") {
          file("Fizz.kt")
        }
        dir("task1") {
          file("task.md")
        }
        dir("task2") {
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

  @Test
  fun `test course with framework lesson structure creation in CC mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
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

  @Test
  fun `test course with sections creation`() {
    courseWithFiles {
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
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt")
        }
      }
    }

    checkFileTree {
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
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test course creation in CC mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt")
        }
      }
    }

    checkFileTree {
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
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test course with sections creation in CC mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt")
        }
      }
    }

    checkFileTree {
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
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test creation of course with sections and without top level lessons in CC mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
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
    }

    checkFileTree {
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
  fun `test empty course creation`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {}

    checkFileTree {
      dir("lesson1") {
        dir("task1") {
          file("Task.txt")
          dir("tests") {
            file("Tests.txt")
          }
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test placeholder content`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("Fizz.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
        eduTask {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt", """fun fooBar(): String = <p>""</p> + <p>""</p>""") {
            placeholder(0, "\"Foo\"")
            placeholder(1, "\"Bar\"")
          }
        }
      }
    }

    checkFileTree {
      dir("lesson1") {
        dir("task1") {
          file("Fizz.kt", code = "fun foo(): String = TODO()")
          file("task.md")
        }
        dir("task2") {
          file("Buzz.kt", code = "fun bar(): String = TODO()")
          file("task.md")
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("FizzBuzz.kt", code = "fun fooBar(): String = \"\" + \"\"")
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test placeholder content in CC mode`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Fizz.kt", "fun foo(): String = <p>TODO()</p>") {
            placeholder(0, "\"Foo\"")
          }
        }
        eduTask {
          taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
            placeholder(0, "\"Bar\"")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("FizzBuzz.kt", """fun fooBar(): String = <p>""</p> + <p>""</p>""") {
            placeholder(0, "\"Foo\"")
            placeholder(1, "\"Bar\"")
          }
        }
      }
    }

    checkFileTree {
      dir("lesson1") {
        dir("task1") {
          file("Fizz.kt", code = "fun foo(): String = \"Foo\"")
          file("task.md")
        }
        dir("task2") {
          file("Buzz.kt", code = "fun bar(): String = \"Bar\"")
          file("task.md")
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("FizzBuzz.kt", code = "fun fooBar(): String = \"Foo\" + \"Bar\"")
          file("task.md")
        }
      }
    }
  }

  @Test
  fun `test placeholder content in CC mode with sections`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson {
          eduTask {
            taskFile("Fizz.kt", "fun foo(): String = <p>TODO()</p>") {
              placeholder(0, "\"Foo\"")
            }
          }
          eduTask {
            taskFile("Buzz.kt", "fun bar(): String = <p>TODO()</p>") {
              placeholder(0, "\"Bar\"")
            }
          }
        }
        lesson {
          eduTask {
            taskFile("FizzBuzz.kt", """fun fooBar(): String = <p>""</p> + <p>""</p>""") {
              placeholder(0, "\"Foo\"")
              placeholder(1, "\"Bar\"")
            }
          }
        }
      }
    }

    checkFileTree {
      dir("section1") {
        dir("lesson1") {
          dir("task1") {
            file("Fizz.kt", code = "fun foo(): String = \"Foo\"")
            file("task.md")
          }
          dir("task2") {
            file("Buzz.kt", code = "fun bar(): String = \"Bar\"")
            file("task.md")
          }
        }
        dir("lesson2") {
          dir("task1") {
            file("FizzBuzz.kt", code = "fun fooBar(): String = \"Foo\" + \"Bar\"")
            file("task.md")
          }
        }
      }
    }
  }
}
