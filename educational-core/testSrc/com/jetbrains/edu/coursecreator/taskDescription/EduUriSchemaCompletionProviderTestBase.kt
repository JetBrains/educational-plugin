package com.jetbrains.edu.coursecreator.taskDescription

import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.codeInsight.EduCompletionTextFixture
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduUriSchemaCompletionProviderTestBase : EduTestCase() {

  protected abstract val taskDescriptionFormat: DescriptionFormat

  private lateinit var completionFixture: EduCompletionTextFixture

  override fun setUp() {
    super.setUp()
    completionFixture = EduCompletionTextFixture(myFixture)
    completionFixture.setUp()
  }

  override fun tearDown() {
    completionFixture.tearDown()
    super.tearDown()
  }

  override fun createCourse() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1", taskDescriptionFormat = taskDescriptionFormat) {
          taskFile("Task.txt")
        }
      }
    }
  }

  protected open fun doTest(before: String, after: String) {
    val name = when (taskDescriptionFormat) {
      DescriptionFormat.HTML -> EduNames.TASK_HTML
      DescriptionFormat.MD -> EduNames.TASK_MD
    }

    val taskDescriptionFile = findFile("lesson1/task1/$name")
    completionFixture.doSingleCompletion(taskDescriptionFile, before, after)
  }
}
