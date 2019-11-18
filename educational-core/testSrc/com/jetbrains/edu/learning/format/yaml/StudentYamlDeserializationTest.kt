package com.jetbrains.edu.learning.format.yaml

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE_WITH_FILE_IO
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.VideoSource
import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeLesson
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
      |""".trimMargin()
    val course = STUDENT_MAPPER.deserializeCourse(yamlContent)
    assertNotNull(course)
    assertEquals(EduNames.STUDY, course.courseMode)
  }

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

  fun `test codeforces task`() {
    val feedbackUrl = "https://codeforces.com/contest/1218/problem/A?locale=en"
    val status = CheckStatus.Unchecked

    val yamlContent = """
    |type: $CODEFORCES_TASK_TYPE
    |feedback_link: $feedbackUrl
    |status: $status
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertNotNull(task)
    assertInstanceOf(task, CodeforcesTask::class.java)
    assertEquals(feedbackUrl, task.feedbackLink.link)
    assertEquals(status, task.status)
  }

  fun `test codeforces task with file io`() {
    val taskFileName = "src/Task.kt"
    val taskSolution = "Task solution"
    val feedbackUrl = "https://codeforces.com/contest/1228/problem/F?locale=ru"
    val status = CheckStatus.Unchecked

    val inputFileName = "in.txt"
    val outputFileName = "out.txt"

    val yamlContent = """
    |type: $CODEFORCES_TASK_TYPE_WITH_FILE_IO
    |files:
    |- name: $taskFileName
    |  visible: true
    |  text: $taskSolution
    |  learner_created: false
    |feedback_link: $feedbackUrl
    |status: $status
    |input_file: $inputFileName
    |output_file: $outputFileName
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertNotNull(task)
    assertInstanceOf(task, CodeforcesTaskWithFileIO::class.java)

    val taskFile = task.taskFiles[taskFileName]!!
    assertEquals(true, taskFile.isVisible)
    assertEquals(taskSolution, taskFile.text)

    assertEquals(feedbackUrl, task.feedbackLink.link)
    assertEquals(status, task.status)

    assertEquals(inputFileName, (task as CodeforcesTaskWithFileIO).inputFileName)
    assertEquals(outputFileName, task.outputFileName)
  }

  fun `test task status`() {
    val yamlContent = """
    |type: edu
    |status: Solved
    |""".trimMargin()
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
    |selected_options:
    |- 1
    |""".trimMargin()
    val task = deserializeTask(yamlContent)
    assertTrue(task is ChoiceTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(mutableListOf(1), (task as ChoiceTask).selectedVariants)
  }

  fun `test video task sources`() {
    val thumbnail = "https://stepikvideo.blob.core.windows.net/thumbnail/29279.jpg"
    val firstSource = VideoSource("https://stepikvideo.blob.core.windows.net/video/29279/1080/f3d83.mp4", "1080")
    val secondSource = VideoSource("https://stepikvideo.blob.core.windows.net/video/29279/720/8c1aa1.mp4", "720")

    val yamlContent = """
    |type: video
    |thumbnail: ${thumbnail}
    |sources:
    |- src: ${firstSource.src}
    |  res: ${firstSource.res}
    |  type: ${firstSource.type}
    |  label: ${firstSource.label}
    |- src: ${secondSource.src}
    |  res: ${secondSource.res}
    |  type: ${secondSource.type}
    |  label: ${secondSource.label}
    |currentTime: 0
    |status: Solved
    |record: 1
    |""".trimMargin("|")

    val task = deserializeTask(yamlContent)
    assertTrue(task is VideoTask)
    assertEquals(CheckStatus.Solved, task.status)
    assertEquals(1, task.record)
    assertEquals(2, (task as VideoTask).sources.size)
    assertEquals(firstSource.src, task.sources[0].src)
    assertEquals(firstSource.res, task.sources[0].res)
    assertEquals(firstSource.label, task.sources[0].label)
    assertEquals(firstSource.type, task.sources[0].type)
    assertEquals(secondSource.src, task.sources[1].src)
    assertEquals(secondSource.res, task.sources[1].res)
    assertEquals(secondSource.label, task.sources[1].label)
    assertEquals(secondSource.type, task.sources[1].type)
    assertEquals(thumbnail, task.thumbnail)
    assertEquals(0, task.currentTime)
  }

  fun `test task record`() {
    val yamlContent = """
    |type: edu
    |record: 1
    |""".trimMargin()
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
    |  learner_created: true
    |""".trimMargin()

    val task = deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles.values.first()
    assertEquals("text", taskFile.text)
    assertTrue(taskFile.isLearnerCreated)
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

  private fun deserializeTask(yamlContent: String) = STUDENT_MAPPER.deserializeTask(yamlContent)
}