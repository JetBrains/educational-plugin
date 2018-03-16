package com.jetbrains.edu.learning

import com.intellij.testFramework.LightPlatformTestCase

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

    val expectedFileTree = fileTree {
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

    expectedFileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}
