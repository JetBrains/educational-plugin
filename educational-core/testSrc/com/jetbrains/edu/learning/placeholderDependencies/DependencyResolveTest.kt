package com.jetbrains.edu.learning.placeholderDependencies

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholderDependency


class DependencyResolveTest : EduTestCase() {
  fun `test resolve dependency`() {
    courseWithFiles {
      lesson("Introduction") {
        eduTask("Hello, world!") {
          taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "Introduction#Hello, world!#Task.kt#1")
          }
        }
      }
    }
    val course = getCourse()
    val placeholderDependency = findPlaceholder(1, 0, "Task.kt", 1).placeholderDependency

    val targetPlaceholder = placeholderDependency!!.resolve(course)!!
    checkPlaceholder(11, 8, targetPlaceholder)
  }

  fun `test no such placeholder`() {
    try {
      courseWithFiles {
        lesson {
          eduTask {
            taskFile("Task.kt", """
          |def f():
          |  print(1)
        """.trimMargin("|"))
          }
        }
        lesson {
          eduTask {
            taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
              placeholder(1, dependency = "lesson1#task1#Task.kt#1")
            }
          }
        }
      }
      fail("Exception is expected")
    }
    catch (e: AnswerPlaceholderDependency.InvalidDependencyException) {
      println("Exception is correctly thrown")
    }
  }

  fun `test resolve to task file in directory`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("com/test/edu/Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Main.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#com/test/edu/Task.kt#1")
          }
        }
      }
    }
    val course = getCourse()
    val placeholderDependency = findPlaceholder(1, 0, "Main.kt", 1).placeholderDependency

    val targetPlaceholder = placeholderDependency!!.resolve(course)!!
    checkPlaceholder(11, 8, targetPlaceholder)
  }

  fun `test resolve path with backslash`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("com/edu/test/Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "lesson1#task1#com\\edu\\test\\Task.kt#1")
          }
        }
      }
    }
    val course = getCourse()
    val placeholderDependency = findPlaceholder(1, 0, "Task.kt", 1).placeholderDependency

    val targetPlaceholder = placeholderDependency!!.resolve(course)!!
    checkPlaceholder(11, 8, targetPlaceholder)
  }

  fun `test resolve with section`() {
    courseWithFiles {
      section("First section") {
        lesson("Introduction") {
          eduTask("Hello, world!") {
            taskFile("Task.kt", """
          |def f():
          |  <p>print(1)</p>
        """.trimMargin("|"))
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Task.kt", """
          |def <p>foo</p>():
          |  <p>type here</p>
          """.trimMargin("|")) {
            placeholder(1, dependency = "First section#Introduction#Hello, world!#Task.kt#1")
          }
        }
      }
    }

    val placeholderDependency = findPlaceholder(0, 0, "Task.kt", 1).placeholderDependency
    val targetPlaceholder = placeholderDependency!!.resolve(getCourse())!!
    checkPlaceholder(11, 8, targetPlaceholder)
  }

  private fun checkPlaceholder(expectedOffset: Int, expectedLength: Int, actualPlaceholder: AnswerPlaceholder) {
    val actualOffset = actualPlaceholder.offset
    val actualLength = actualPlaceholder.realLength
    assertTrue("Resolved to wrong placeholder. Expected offset=$expectedOffset, length=$expectedLength, " +
                        "but got offset=$actualOffset, length=$actualLength",
                        expectedOffset == actualOffset && expectedLength == expectedLength)
  }
}