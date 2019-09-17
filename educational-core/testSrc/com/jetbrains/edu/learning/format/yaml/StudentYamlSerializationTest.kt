package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class StudentYamlSerializationTest : EduTestCase()  {

  fun `test student course`() {
    val course = course {}

    doTest(course, """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |mode: Study
      |
    """.trimMargin("|"))
  }


  fun `test task`() {
    val task = courseWithFiles {
      lesson {
        eduTask()
      }
    }.lessons.first().taskList.first()
    task.status = CheckStatus.Solved
    task.record = 1

    doTest(task, """
    |type: edu
    |status: Solved
    |record: 1
    |""".trimMargin("|"))
  }

  fun `test choice task`() {
    val task: ChoiceTask = courseWithFiles {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT))
      }
    }.lessons.first().taskList.first() as ChoiceTask
    task.status = CheckStatus.Solved
    task.record = 1
    task.selectedVariants = mutableListOf(1)

    doTest(task, """
    |type: choice
    |is_multiple_choice: false
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |message_correct: Congratulations!
    |message_incorrect: Incorrect solution
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |""".trimMargin("|"))
  }

  fun `test checkio mission`() {
    val checkiOMission = CheckiOMission()
    checkiOMission.code = "code"

    doTest(checkiOMission, """
    |type: checkio
    |status: Unchecked
    |record: -1
    |code: code
    |seconds_from_change: 0
    |
    """.trimMargin("|"))
  }

  fun `test task with task files`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "text")
        }
      }
    }.lessons.first().taskList.first()

    doTest(task, """
    |type: edu
    |files:
    |- name: task.txt
    |  visible: true
    |  text: text
    |status: Unchecked
    |record: -1
    |""".trimMargin("|"))
  }

  fun `test task with placeholders`() {
    val task = courseWithFiles {
      lesson {
        eduTask {
          taskFile("task.txt", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "")
          }
        }
      }
    }.lessons.first().taskList.first()

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
    |    selected: false
    |    status: Unchecked
    |  text: 42 is the answer
    |status: Unchecked
    |record: -1
    |""".trimMargin("|"))
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.STUDENT_MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}