package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.codeInsight.EduCompletionTextFixture

abstract class EduUriSchemaCompletionProviderTestBase : EduTaskDescriptionTestBase() {

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
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.doSingleCompletion(taskDescriptionFile, before, after)
  }
}
