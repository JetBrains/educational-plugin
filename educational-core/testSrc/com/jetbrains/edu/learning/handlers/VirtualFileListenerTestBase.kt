package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.handlers.CCVirtualFileListenerTest
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduTestDialog
import com.jetbrains.edu.learning.FileCheck
import com.jetbrains.edu.learning.configurators.FakeGradleBasedLanguage
import com.jetbrains.edu.learning.configurators.FakeGradleConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.withTestDialog

abstract class VirtualFileListenerTestBase : EduTestCase() {

  private lateinit var listener: EduVirtualFileListener

  protected abstract val courseMode: String
  protected abstract fun createListener(project: Project): EduVirtualFileListener

  override fun setUp() {
    super.setUp()
    listener = createListener(project)
    VirtualFileManager.getInstance().addVirtualFileListener(listener)
  }

  override fun tearDown() {
    super.tearDown()
    VirtualFileManager.getInstance().removeVirtualFileListener(listener)
  }
  
  protected fun doAddFileTest(filePathInTask: String, checksProducer: (Task) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage,
      createYamlConfigs = true
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/Task.kt")
        }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getTaskDir(project) ?: error("Failed to find directory of `${task.name}` task")

    GeneratorUtils.createChildFile(taskDir, filePathInTask, "")
    checksProducer(task).forEach(FileCheck::check)
  }

  protected fun doRemoveFileTest(filePathInCourse: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("src/TaskFile.kt")
          taskFile("additionalFile.txt")
          taskFile("test/${FakeGradleConfigurator.TEST_FILE_NAME}")
          dir("src/packageName") {
            taskFile("TaskFile2.kt")
            taskFile("TaskFile3.kt")
          }
          dir("additional_files") {
            taskFile("additional_file2.txt")
            taskFile("additional_file3.txt")
          }
          dir("test/packageName") {
            taskFile("Tests2.kt")
            taskFile("Tests3.kt")
          }
        }
      }
      section("section1") {
        lesson("lesson2")
      }
    }

    val file = findFile(filePathInCourse)
    runWriteAction { file.delete(CCVirtualFileListenerTest::class.java) }
    checksProducer(course).forEach(FileCheck::check)
  }

  protected fun doRenameFileTest(filePathInCourse: String, newName: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src/packageName") {
            taskFile("Task1.kt")
            taskFile("Task2.kt")
          }

          dir("additional_files") {
            taskFile("additional_file1.txt")
            taskFile("additional_file2.txt")
          }
          dir("test/packageName") {
            taskFile("Test1.kt")
            taskFile("Test2.kt")
          }
        }
      }
    }

    val file = findFile(filePathInCourse)
    val psiFileSystemItem = if (file.isDirectory) {
      PsiManager.getInstance(project).findDirectory(file) ?: error("Can't find psi directory for $file")
    } else {
      PsiManager.getInstance(project).findFile(file) ?: error("Can't find psi file for $file")
    }
    myFixture.renameElement(psiFileSystemItem, newName)
    val checks = checksProducer(course)
    checks.forEach(FileCheck::check)

    val dialog = EduTestDialog()
    withTestDialog(dialog) {
      UndoManager.getInstance(project).undo(null)
    }
    checks.map(FileCheck::invert).forEach(FileCheck::check)
    withTestDialog(dialog) {
      UndoManager.getInstance(project).redo(null)
    }
    checks.forEach(FileCheck::check)
  }

  protected fun doMoveTest(filePath: String, newParentPath: String, checksProducer: (Course) -> List<FileCheck>) {
    val course = courseWithFiles(
      courseMode = courseMode,
      language = FakeGradleBasedLanguage
    ) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src") {
            taskFile("Task1.kt")
            dir("foo") {
              taskFile("Task2.kt")
              taskFile("Task3.kt")
            }
            taskFile("bar/Task4.kt")
          }

          taskFile("additional_file1.txt")
          dir("foo") {
            taskFile("additional_file2.txt")
            taskFile("additional_file3.txt")
          }
          taskFile("bar/additional_file4.txt")

          dir("test") {
            taskFile("Tests1.kt")
            dir("foo") {
              taskFile("Tests2.kt")
              taskFile("Tests3.kt")
            }
            taskFile("bar/Tests4.kt")
          }
        }
      }
    }

    val requestor = CCVirtualFileListenerTest::class.java

    runWriteAction {
      val dir = LightPlatformTestCase.getSourceRoot()
        .createChildDirectory(requestor, "non_course_dir")
      dir.createChildData(requestor, "non_course_file1.txt")
      dir.createChildData(requestor, "non_course_file2.txt")
    }

    val file = findFile(filePath)
    val newParent  = findFile(newParentPath)
    runWriteAction { file.move(requestor, newParent) }

    checksProducer(course).forEach(FileCheck::check)
  }
}
