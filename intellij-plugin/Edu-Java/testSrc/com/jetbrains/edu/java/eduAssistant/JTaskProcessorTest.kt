package com.jetbrains.edu.java.eduAssistant

import com.jetbrains.edu.jvm.slow.checker.JdkCheckerTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.eduAssistant.processors.TaskProcessor
import com.jetbrains.edu.learning.findTask
import org.junit.Test

class JTaskProcessorTest : JdkCheckerTestBase() {

  @Test
  fun testFunctionsSetTextRepresentationInJavaProject() {
    val course = project.course ?: error("Course was not found")
    val task = course.findTask("lesson1", "task1")
    val taskProcessor = TaskProcessor(task)
    val functionSet = taskProcessor.getFunctionsFromTask()?.toSet()?.map { it.toString() }
    val expected = listOf("invokeSayHello(howManyTimes: int): String", "main(args: String[]): void")
    assertEquals(expected, functionSet)
    assertEquals(
      listOf("invokeSayHello(howManyTimes: int): String", "main(args: String[]): void"),
      task.taskFiles["src/main/kotlin/Main.java"]?.functionSignatures?.toSet()?.map { it.toString() }
    )
  }

  override fun createCourse(): Course = javaCourse
}