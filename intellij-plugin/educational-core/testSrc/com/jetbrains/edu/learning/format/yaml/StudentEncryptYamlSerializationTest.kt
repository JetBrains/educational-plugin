package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.cipher.TestCipher
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.yaml.YamlMapper
import org.junit.Test

class StudentEncryptYamlSerializationTest : EduTestCase() {

  @Test
  fun `test task with task files`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "text")
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  encrypted_text: fgnGdWA8h6P1G1byNm3P3g==
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test edu task in student mode with encrypted text`() {
    val task = course(courseMode = CourseMode.STUDENT) {
      lesson {
        eduTask {
          taskFile("Test.txt", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.txt
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |    initialized_from_dependency: false
    |    encrypted_possible_answer: 6zkm3NpDQQaIQ+CAebF//w==
    |    selected: false
    |    status: Unchecked
    |  encrypted_text: lrKTY22nc3exEO7HQjXPxaXf97REIR5R1llqKFTGca0=
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test task with placeholders`() {
    val taskSolution = "42 is the answer"
    val possibleAnswer = "answer"
    val cipher = TestCipher()
    val encryptedPossibleAnswer = cipher.encrypt(possibleAnswer)
    val taskSolutionEncrypted = cipher.encrypt(taskSolution)
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "<p>$taskSolution</p>") {
            placeholder(0, placeholderText = "", possibleAnswer = possibleAnswer)
          }
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: ""
    |    initial_state:
    |      length: 16
    |      offset: 0
    |    initialized_from_dependency: false
    |    encrypted_possible_answer: $encryptedPossibleAnswer
    |    selected: false
    |    status: Unchecked
    |  encrypted_text: $taskSolutionEncrypted
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test learner created`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "text")
        }
      }
    }.findTask("lesson1", "task1")
    task.taskFiles.values.first().isLearnerCreated = true

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  encrypted_text: fgnGdWA8h6P1G1byNm3P3g==
    |  learner_created: true
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test no text for invisible task file`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "task text", false)
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: false
    |  encrypted_text: Bb6BVFFg7T7oP1LtfAFuEg==
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  private fun doTest(task: Task, expected: String) {
    try {
      task.course.needWriteYamlText = true
      val studentMapper = YamlMapper.testStudentMapperWithEncryption()
      studentMapper.registerModule(EncryptionModule(TestCipher()))
      val actual = studentMapper.writeValueAsString(task)
      assertEquals(expected, actual)
    }
    finally {
      task.course.needWriteYamlText = false
    }
  }
}