package com.jetbrains.edu.codeInsight.taskDescription

import com.jetbrains.edu.codeInsight.EduCompletionTextFixture
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat

abstract class EduTaskDescriptionCompletionTestBase(format: DescriptionFormat) : EduTaskDescriptionTestBase(format) {

  private lateinit var completionFixture: EduCompletionTextFixture

  override fun setUp() {
    super.setUp()
    completionFixture = EduCompletionTextFixture(myFixture)
    completionFixture.setUp()
    registerTaskDescriptionToolWindow()
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
          taskFile("foo/Task2.txt")
          dir("tests") {
            taskFile("test.txt")
          }
        }
      }
      section("section1") {
        lesson("lesson2") {
          eduTask("tas2", taskDescriptionFormat = taskDescriptionFormat) {
            taskFile("Task.txt")
          }
        }
      }
    }
  }

  protected open fun doTest(linkBefore: String, linkAfter: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.doSingleCompletion(taskDescriptionFile, linkBefore.withDescriptionFormat(), linkAfter.withDescriptionFormat())
  }

  protected open fun checkNoCompletion(link: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.checkNoCompletion(taskDescriptionFile, link.withDescriptionFormat())
  }

  protected fun checkDoNotContainCompletion(link: String, variant: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    completionFixture.checkDoNotContainCompletion(taskDescriptionFile, link.withDescriptionFormat(), variant)
  }
}
