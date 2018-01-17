package com.jetbrains.edu.coursecreator.configuration

import com.jetbrains.edu.coursecreator.CCTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Test


class YamlDeserializationTest : CCTestCase() {

  @Test
  fun `test edu task synchronization`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val taskBeforeSynchronization = course.lessons[0].taskList[0]!!
    configureByTaskFile("taskFile.txt")
    CourseInfoSynchronizer.testTaskLoading(project, taskBeforeSynchronization.getTaskDir(project)!!, """
      |type: edu
      |task_files:
      |- name: taskFile.txt
      |  placeholders:
      |  - offset: 30
      |    length: 11
      |    hints: []
      |    placeholder_text: type here
      |""".trimMargin("|"))
    val taskAfterSynchronization = course.lessons[0].taskList[0]!!
    assertTrue(taskAfterSynchronization is EduTask)
    val answerPlaceholder = taskAfterSynchronization.getTaskFiles()["taskFile.txt"]!!.answerPlaceholders[0]!!
    assertEquals(30, answerPlaceholder.offset)
    assertEquals(11, answerPlaceholder.realLength)
    assertEmpty(answerPlaceholder.hints)
    assertEquals("type here", answerPlaceholder.placeholderText)
    assertEquals("placeholder", answerPlaceholder.possibleAnswer)
  }


  @Test
  fun `test task type change`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val taskBeforeSynchronization = course.lessons[0].taskList[0]!!
    configureByTaskFile("taskFile.txt")
    val taskDir = taskBeforeSynchronization.getTaskDir(project)!!
    CourseInfoSynchronizer.testTaskLoading(project, taskDir, """
      |type: output
      |task_files:
      |- name: taskFile.txt
      |""".trimMargin("|"))
    val taskAfterSynchronization = course.lessons[0].taskList[0]!!
    assertEquals(taskAfterSynchronization::class.java, OutputTask::class.java)
  }

  override fun getBasePath(): String {
    return super.getBasePath() + "/format/yaml"
  }
}