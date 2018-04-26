package com.jetbrains.edu.learning

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils

class PlainTextCourseGeneratorTest : EduTestCase() {

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
        }
        dir("task2") {
          file("Buzz.kt")
        }
      }
      dir("lesson2") {
        dir("task1") {
          file("FizzBuzz.kt")
        }
      }
    }
  }

  fun `test course with framework lesson structure creation`() {
    courseWithFiles {
      lesson(isFramework = true) {
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
      }
      dir("lesson2") {
        dir("task1") {
          file("FizzBuzz.kt")
        }
      }
    }
  }

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
          }
          dir("task2") {
            file("Buzz.kt")
          }
        }
      }
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
        }
      }
    }
  }

  fun `test course creation in CC mode`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
            file("task.html")
          }
          dir("task2") {
            file("Buzz.kt")
            file("task.html")
          }
        }
      }
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
          file("task.html")
        }
      }
    }
  }

  fun `test course with sections creation in CC mode`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
            file("task.html")
          }
          dir("task2") {
            file("Buzz.kt")
            file("task.html")
          }
        }
      }
      dir("lesson1") {
        dir("task1") {
          file("FizzBuzz.kt")
          file("task.html")
        }
      }
    }
  }

  fun `test creation of course with sections and without top level lessons in CC mode`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
            file("task.html")
          }
          dir("task2") {
            file("Buzz.kt")
            file("task.html")
          }
        }
        dir("lesson2") {
          dir("task1") {
            file("FizzBuzz.kt")
            file("task.html")
          }
        }
      }
    }
  }

  fun `test empty course creation`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {}

    checkFileTree {
      dir("lesson1") {
        dir("task1") {
          file("Task.txt")
          file("task.html")
        }
      }
    }
  }

  private fun checkFileTree(block: FileTreeBuilder.() -> Unit) {
    fileTree(block).assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}
