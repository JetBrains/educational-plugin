package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOMission
import com.jetbrains.edu.learning.courseFormat.checkio.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.TableTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.MatchingTask
import com.jetbrains.edu.learning.courseFormat.tasks.matching.SortingTask
import com.jetbrains.edu.learning.format.doTestPlaceholderAndDependencyVisibility
import com.jetbrains.edu.learning.messages.EduFormatBundle
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeLesson
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeTask
import com.jetbrains.edu.learning.yaml.YamlMapper.STUDENT_MAPPER
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.util.*

class StudentYamlDeserializationTest : EduTestCase() {

  @Test
  fun `test course mode`() {
    val yamlContent = """
      |title: Test Course
      |mode: Study
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |""".trimMargin()
    val course = STUDENT_MAPPER.deserializeCourse(yamlContent)
    assertNotNull(course)
    assertEquals(CourseMode.STUDENT, course.courseMode)
  }

  @Test
  fun `test marketplace course type`() {
    val yamlContent = """
      |type: marketplace
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |""".trimMargin()
    val course = STUDENT_MAPPER.deserializeCourse(yamlContent)
    assertNotNull(course)
    assertTrue(course is EduCourse)
  }

  @Test
  fun `test checkio station`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: checkiO
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = STUDENT_MAPPER.deserializeLesson(yamlContent)
    assertTrue(lesson is CheckiOStation)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
  }

  @Test
  fun `test checkio mission`() {
    val yamlContent = """
    |type: checkiO
    |status: Unchecked
    |record: -1
    |code: code
    |seconds_from_change: 1
    |
    """.trimMargin()
    val task = STUDENT_MAPPER.deserializeTask(yamlContent)
    assertNotNull(task)
    assertInstanceOf(task, CheckiOMission::class.java)
    assertEquals("code", (task as CheckiOMission).code)
    assertEquals(1, task.secondsFromLastChangeOnServer)
  }

  @Test
  fun `test framework lessons`() {
    val yamlContent = """
    |type: framework
    |content:
    | - task1
    | - task2
    |current_task: 1
    |
    """.trimMargin("|")
    val lesson = STUDENT_MAPPER.deserializeLesson(yamlContent)
    assertNotNull(lesson)
    assertInstanceOf(lesson, FrameworkLesson::class.java)
    assertEquals(1, (lesson as FrameworkLesson).currentTaskIndex)

  }

  @Test
  fun `test task status`() {
    val yamlContent = """
    |type: edu
    |status: Solved
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(CheckStatus.Solved, task.status)
  }

  @Test
  fun `test task with feedback`() {
    val message = "My error message"
    val time = Date(0)
    val expected = "A"
    val actual = "B"
    val yamlContent = """
    |type: edu
    |status: Failed
    |feedback:
    |  message: $message
    |  time: Thu, 01 Jan 1970 00:00:00 UTC
    |  expected: $expected
    |  actual: $actual
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(CheckStatus.Failed, task.status)
    assertEquals(CheckFeedback(message, time, expected, actual), task.feedback)
  }

  @Test
  fun `test task with incomplete feedback`() {
    val time = Date(0)
    val yamlContent = """
    |type: edu
    |status: Failed
    |feedback:
    |  time: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(CheckStatus.Failed, task.status)
    assertEquals(CheckFeedback("", time), task.feedback)
  }

  @Test
  fun `test selected variants`() {
    val yamlContent = """
    |type: choice
    |is_multiple_choice: false
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(mutableListOf(1), (task as ChoiceTask).selectedVariants)
    assertEquals(EduFormatBundle.message("check.correct.solution"), task.messageCorrect)
    assertEquals(EduFormatBundle.message("check.incorrect.solution"), task.messageIncorrect)
    assertEquals(EduFormatBundle.message("course.creator.create.choice.task.single.label"), task.presentableQuizHeader)
  }

  @Test
  fun `test multiple choice task label`() {
    val yamlContent = """
    |type: choice
    |is_multiple_choice: true
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |- 2
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(mutableListOf(1, 2), (task as ChoiceTask).selectedVariants)
    assertEquals(EduFormatBundle.message("check.correct.solution"), task.messageCorrect)
    assertEquals(EduFormatBundle.message("check.incorrect.solution"), task.messageIncorrect)
    assertEquals(EduFormatBundle.message("course.creator.create.choice.task.multiple.label"), task.presentableQuizHeader)
  }

  @Test
  fun `test can check locally`() {
    val yamlContent = """
    |type: choice
    |is_multiple_choice: false
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |local_check: false
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(false, (task as ChoiceTask).canCheckLocally)
    assertEquals(mutableListOf(1), task.selectedVariants)
    assertEquals(EduFormatBundle.message("check.correct.solution"), task.messageCorrect)
    assertEquals(EduFormatBundle.message("check.incorrect.solution"), task.messageIncorrect)
    assertEquals(EduFormatBundle.message("course.creator.create.choice.task.single.label"), task.presentableQuizHeader)
  }

  @Test
  fun `test deserialize choice task with custom messages variants`() {
    val yamlContent = """
    |type: choice
    |is_multiple_choice: false
    |options:
    |- text: 1
    |  is_correct: true
    |- text: 2
    |  is_correct: false
    |message_correct: You are good!
    |message_incorrect: You are not good!
    |quiz_header: I am a topic
    |status: Solved
    |record: 1
    |selected_options:
    |- 1
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(mutableListOf(1), (task as ChoiceTask).selectedVariants)
    assertEquals("I am a topic", task.presentableQuizHeader)
    assertEquals("You are good!", task.messageCorrect)
    assertEquals("You are not good!", task.messageIncorrect)
  }

  @Test
  fun `test deserialize sorting task`() {
    val yamlContent = """
    |type: sorting
    |options:
    |- first
    |- second
    |status: Solved
    |record: 1
    |ordering:
    |- 1
    |- 0
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    check(task is SortingTask)
    assertEquals(task.record, 1)
    assertEquals(task.ordering.toList(), listOf(1, 0))
    assertEquals(task.status, CheckStatus.Solved)
    assertEquals(task.options, listOf("first", "second"))
  }

  @Test
  fun `test deserialize matching task`() {
    val yamlContent = """
    |type: matching
    |captions:
    |- dog
    |- cat
    |options:
    |- first
    |- second
    |status: Solved
    |record: 1
    |ordering:
    |- 1
    |- 0
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    check(task is MatchingTask)
    assertEquals(task.captions, listOf("dog", "cat"))
    assertEquals(task.ordering.toList(), listOf(1, 0))
    assertEquals(task.options, listOf("first", "second"))
  }

  @Test
  fun `test deserialize table task`() {
    val yamlContent = """
    |type: table
    |status: Solved
    |record: 1
    |rows:
    |- A
    |- B
    |columns:
    |- 1
    |- 2
    |- 3
    |selected:
    |- - false
    |  - true
    |  - false
    |- - false
    |  - false
    |  - true
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    check(task is TableTask)
    assertEquals(task.rows, listOf("A", "B"))
    assertEquals(task.columns, listOf("1", "2", "3"))
    assertEquals(task.selected.map { it.toList() }.toList(), listOf(listOf(false, true, false), listOf(false, false, true)))
  }

  @Test
  fun `test task record`() {
    val yamlContent = """
    |type: edu
    |record: 1
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(1, task.record)
  }

  @Test
  fun `test task file text`() {
    val taskFileName = "Task.java"
    val yamlContent = """
    |type: edu
    |files:
    |- name: $taskFileName
    |  visible: true
    |  text: text
    |  learner_created: true
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    assertEquals("text", taskFile.text)
    assertTrue(taskFile.isLearnerCreated)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    val initialState = placeholder.initialState
    assertNotNull("Initial state is null", initialState)
    assertEquals(0, initialState.offset)
    assertEquals(1, initialState.length)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(true, placeholder.isInitializedFromDependency)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals("answer", placeholder.possibleAnswer)
  }

  @Test
  fun `test placeholder no possible answer`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
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
    assertEquals("", placeholder.possibleAnswer)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(true, placeholder.selected)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals(CheckStatus.Solved, placeholder.status)
  }

  @Test
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
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    val placeholder = taskFile.answerPlaceholders.first()
    assertEquals("student answer", placeholder.studentAnswer)
  }

  @Test
  fun `test task file editable`() {
    val taskFileName = "Task.java"
    val yamlContent = """
    |type: edu
    |files:
    |- name: $taskFileName
    |  visible: true
    |  editable: false
    |  text: text
    |  learner_created: true
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    assertEquals("text", taskFile.text)
    assertTrue(taskFile.isLearnerCreated)
    assertFalse(taskFile.isEditable)
  }

  @Test
  fun `test code task with java17`() {
    testCodeTaskWithProgrammingLanguage("java17")
  }

  @Test
  fun `test code task with c++`() {
    testCodeTaskWithProgrammingLanguage("c++")
  }

  @Test
  fun `test code task with python3_10`() {
    testCodeTaskWithProgrammingLanguage("python3.10")
  }

  @Test
  fun `test code task with scala`() {
    testCodeTaskWithProgrammingLanguage("scala")
  }

  private fun testCodeTaskWithProgrammingLanguage(programmingLanguage: String) {
    val yamlContent = getYAMLWithProgrammingLanguage(programmingLanguage)
    val task = deserializeTask(yamlContent)
    assertInstanceOf(task, CodeTask::class.java)
    task as CodeTask
    assertEquals(programmingLanguage, task.submissionLanguage)
  }

  @Language("YAML")
  private fun getYAMLWithProgrammingLanguage(programmingLanguage: String): String {
    return """
    |type: code
    |custom_name: Code task
    |files:
    |- name: src/Main.java
    |  visible: true
    |  text: |-
    |    import java.util.Scanner;
    |
    |    public class Main {
    |        public static void main(String[] args) {
    |            Scanner scanner = new Scanner(System.in);
    |            int number = scanner.nextInt();
    |            System.out.println(number);
    |        }
    |    }
    |  learner_created: false
    |status: Failed
    |submission_language: $programmingLanguage
    |""".trimMargin()
  }

  @Test
  fun `test reading binary files from YAML`() {
    val yamlContent = """
    |type: edu
    |files:
    |- name: a.png
    |  visible: true
    |  is_binary: true
    |- name: b.txt
    |  visible: true
    |- name: c.txt
    |  visible: true
    |  text: hello.txt
    |- name: d.png
    |  visible: true
    |""".trimMargin()

    val task = deserializeTask(yamlContent)

    val aPng = task.taskFiles["a.png"]!!
    assertContentsEqual("a.png", BinaryContents.EMPTY, aPng.contents)

    val bTxt = task.taskFiles["b.txt"]!!
    assertContentsEqual("b.txt", TextualContents.EMPTY, bTxt.contents)

    val cTxt = task.taskFiles["c.txt"]!!
    assertContentsEqual("c.txt", InMemoryTextualContents("hello.txt"), cTxt.contents)

    val dPng = task.taskFiles["d.png"]!!
    // d.png is stored in an old way: without the 'is_binary' field.
    // So, when it is read, it is treated as textual, because is_binary is false by default.
    // Binary files never have a 'text' field, so they will be treated as empty textual files.
    // This is not a problem because binary files have always been not working properly.
    // In other words, before the 'is_binary' field was introduced, binary files were broken, and after the 'is_binary' is introduced,
    // they are still broken, but differently.
    assertContentsEqual("d.png", TextualContents.EMPTY, dPng.contents)
  }

  @Test
  fun `test placeholder with invisible dependency`() = doTestPlaceholderAndDependencyVisibility(
    STUDENT_MAPPER.deserializeTask("""
        |type: edu
        |files:
        |- name: Test.java
        |  placeholders:
        |  - offset: 0
        |    length: 3
        |    placeholder_text: 'type here   '
        |    dependency:
        |      lesson: lesson1
        |      task: task1
        |      file: Test.java
        |      placeholder: 1
        |      is_visible: false
        |    possible_answer: answer
        |    status: Solved
        |    student_answer: student answer
        |    initial_state:
        |      offset: 0
        |      length: 1
    """.trimMargin()
    ), expectedPlaceholderVisibility = false)

  @Test
  fun `test invisible placeholder`() = doTestPlaceholderAndDependencyVisibility(
    STUDENT_MAPPER.deserializeTask("""
        |type: edu
        |files:
        |- name: Test.java
        |  placeholders:
        |  - offset: 0
        |    length: 3
        |    placeholder_text: 'type here   '
        |    is_visible: false
        |    possible_answer: answer
        |    status: Solved
        |    student_answer: student answer
        |    initial_state:
        |      offset: 0
        |      length: 1
    """.trimMargin()
    ), expectedPlaceholderVisibility = false)

  @Test
  fun `test placeholder without visibility field in student mode`() = doTestPlaceholderAndDependencyVisibility(
    STUDENT_MAPPER.deserializeTask("""
        |type: edu
        |files:
        |- name: Test.java
        |  placeholders:
        |  - offset: 0
        |    length: 3
        |    placeholder_text: 'type here   '
        |    possible_answer: answer
        |    status: Solved
        |    student_answer: student answer
        |    initial_state:
        |      offset: 0
        |      length: 1
    """.trimMargin()
    ), expectedPlaceholderVisibility = true)

  @Test
  fun `test visible placeholder and invisible dependency`() = doTestPlaceholderAndDependencyVisibility(
    STUDENT_MAPPER.deserializeTask("""
        |type: edu
        |files:
        |- name: Test.java
        |  placeholders:
        |  - offset: 0
        |    length: 3
        |    placeholder_text: 'type here   '
        |    dependency:
        |      lesson: lesson1
        |      task: task1
        |      file: Test.java
        |      placeholder: 1
        |      is_visible: false
        |    is_visible: true
        |    possible_answer: answer
        |    status: Solved
        |    student_answer: student answer
        |    initial_state:
        |      offset: 0
        |      length: 1
    """.trimMargin()
    ), expectedPlaceholderVisibility = false)

  private fun deserializeTask(yamlContent: String) = STUDENT_MAPPER.deserializeTask(yamlContent)
}