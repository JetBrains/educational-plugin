package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.encrypt.AES256
import com.jetbrains.edu.learning.encrypt.getAesKey
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class StudentEncryptYamlSerializationTest : EduTestCase() {

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

  fun `test edu task in student mode with encrypted text`() {
    val task = course(courseMode = EduNames.STUDY) {
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
    |    selected: false
    |    status: Unchecked
    |    encrypted_possible_answer: 6zkm3NpDQQaIQ+CAebF//w==
    |  encrypted_text: lrKTY22nc3exEO7HQjXPxaXf97REIR5R1llqKFTGca0=
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  fun `test task with placeholders`() {
    val taskSolution = "42 is the answer"
    val possibleAnswer = "answer"
    val encryptedPossibleAnswer = AES256.encrypt(possibleAnswer, getAesKey())
    val taskSolutionEncrypted = AES256.encrypt(taskSolution, getAesKey())
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
    |    selected: false
    |    status: Unchecked
    |    encrypted_possible_answer: $encryptedPossibleAnswer
    |  encrypted_text: $taskSolutionEncrypted
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

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

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER_WITH_ENCRYPTION.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}