package com.jetbrains.edu.codeInsight.taskDescription

import com.intellij.psi.PsiManager
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import org.junit.Test

abstract class EduInCourseLinkPathResolveTestBase(format: DescriptionFormat) : EduTaskDescriptionTestBase(format) {

  override fun createCourse() {
    courseWithFiles {
      lesson("lesson1") {
        eduTask("task1", taskDescriptionFormat = taskDescriptionFormat) {
          taskFile("Task.txt")
        }
      }
      lesson("lesson2") {
        eduTask("task2", taskDescriptionFormat = taskDescriptionFormat) {
          taskFile("TaskFile1.txt")
          taskFile("foo/bar/TaskFile2.txt")
        }
      }
      section("section1") {
        lesson("lesson3") {
          eduTask("task3", taskDescriptionFormat = taskDescriptionFormat) {
            taskFile("TaskFile3.txt")
          }
        }
      }
    }
  }

  @Test
  fun `test section reference`() = doTest("course://section<caret>1", "section1")
  @Test
  fun `test lesson reference`() = doTest("course://lesson<caret>2", "lesson2")
  @Test
  fun `test lesson in section reference`() = doTest("course://section1/lesson<caret>3", "section1/lesson3")
  @Test
  fun `test task reference`() = doTest("course://lesson2/task<caret>2", "lesson2/task2")
  @Test
  fun `test task file reference 1`() = doTest("course://lesson2/task2/TaskFile<caret>1.txt", "lesson2/task2/TaskFile1.txt")
  @Test
  fun `test task file reference 2`() = doTest("course://lesson2/task2/foo/bar/TaskFile<caret>2.txt", "lesson2/task2/foo/bar/TaskFile2.txt")

  protected open fun doTest(link: String, expectedPath: String) {
    val taskDescriptionFile = findFile("lesson1/task1/${taskDescriptionFormat.descriptionFileName}")
    myFixture.saveText(taskDescriptionFile, link.withDescriptionFormat())
    myFixture.configureFromExistingVirtualFile(taskDescriptionFile)

    val reference = myFixture.getReferenceAtCaretPositionWithAssertion()
    val resolvedElement = reference.resolve()

    val expectedFile = findFile(expectedPath)
    val psiManager = PsiManager.getInstance(project)
    val expectedElement = psiManager.findFile(expectedFile) ?: psiManager.findDirectory(expectedFile)
    assertEquals(expectedElement, resolvedElement)
  }
}
