package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.STUDENT_MAPPER

class StudentYamlDeserializationTest : EduTestCase() {

  fun `test course mode`() {
    val yamlContent = """
      |title: Test Course
      |mode: Study
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin("|")
    val course = STUDENT_MAPPER.deserializeCourse(yamlContent)
    assertNotNull(course)
    assertEquals(EduNames.STUDY, course!!.courseMode)
  }

  fun `test checkio mission`() {
    val yamlContent = """
    |type: checkio
    |status: Unchecked
    |record: -1
    |code: code
    |seconds_from_change: 1
    |
    """.trimMargin("|")
    val task = STUDENT_MAPPER.deserializeTask(yamlContent)
    assertNotNull(task)
    assertInstanceOf(task, CheckiOMission::class.java)
    assertEquals("code", (task as CheckiOMission).code)
    assertEquals(1, task.secondsFromLastChangeOnServer)
  }

  fun `test task status`() {
    val yamlContent = """
    |type: edu
    |status: Solved
    |""".trimMargin("|")
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(CheckStatus.Solved, task.status)
  }

  fun `test selected variants`() {
    val yamlContent = """
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
    |selected_variants:
    |- 1
    |""".trimMargin("|")
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(mutableListOf(1), (task as ChoiceTask).selectedVariants)
  }

  fun `test task record`() {
    val yamlContent = """
    |type: edu
    |record: 1
    |""".trimMargin("|")
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(1, task.record)
  }

  fun `test task file text`() {
    val taskFileName = "Task.java"
    val yamlContent = """
    |type: edu
    |files:
    |- name: $taskFileName
    |  visible: true
    |  text: text
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals("text", task.taskFiles.values.first().text)
  }

  fun `test placeholder initial state`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    possible_answer: answer
    |    status: Solved
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    val initialState = placeholder.initialState
    assertNotNull("Initial state is null", initialState)
    assertEquals(0, initialState.offset)
    assertEquals(1, initialState.length)
  }

  fun `test placeholder init from dependency`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    initialized_from_dependency: true
    |    status: Solved
    |    possible_answer: answer
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(true, placeholder.isInitializedFromDependency)
  }

  fun `test placeholder possible answer`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    possible_answer: answer
    |    status: Solved
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals("answer", placeholder.possibleAnswer)
  }

  fun `test placeholder selected`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    possible_answer: answer
    |    selected: true
    |    status: Solved
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(true, placeholder.selected)
  }

  fun `test placeholder status`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    possible_answer: answer
    |    status: Solved
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(CheckStatus.Solved, placeholder.status)
  }

  fun `test placeholder student answer`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    possible_answer: answer
    |    status: Solved
    |    student_answer: student answer
    |    initial_state:
    |      offset: 0
    |      length: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals("student answer", placeholder.studentAnswer)
  }

  private fun deserializeTask(yamlContent: String) = STUDENT_MAPPER.deserializeTask(yamlContent)
}