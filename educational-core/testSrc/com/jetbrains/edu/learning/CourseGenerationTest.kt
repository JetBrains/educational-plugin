package com.jetbrains.edu.learning

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestVFiles
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.junit.Assert.assertThat

class CourseGenerationTest : CourseGenerationTestBase<Unit>() {
  override val defaultSettings: Unit = Unit

  fun `test do not open invisible files after course creation`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("invisible.txt", visible = false)
          taskFile("visible.txt")
        }
      }
    }
    createCourseStructure(course)

    val invisible = findFile("lesson1/task1/invisible.txt")
    val visible = findFile("lesson1/task1/visible.txt")
    val openFiles = FileEditorManager.getInstance(project).openFiles.toList()
    assertThat(openFiles, not(hasItem(invisible)))
    assertThat(openFiles, hasItem(visible))
  }

  fun `test substitute placeholder answers in CC mode`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.kt", """
            fun foo(): String = <p>TODO()</p>
            fun bar(): Int = <p>TODO()</p>
            fun baz(): Boolean = <p>TODO()</p>
          """) {
            placeholder(0, "\"\"")
            placeholder(1, "0")
            placeholder(2, "false")
          }
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("TaskFile1.kt", """
          fun foo(): String = ""
          fun bar(): Int = 0
          fun baz(): Boolean = false
        """)
        file("task.html")
      }
    }.assertEquals(rootDir)
  }

  fun `test opened files in CC mode`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("TaskFile1.kt", """
            fun foo(): String = <p>TODO()</p>
            fun bar(): Int = <p>TODO()</p>
            fun baz(): Boolean = <p>TODO()</p>
          """) {
            placeholder(0, "\"\"")
            placeholder(1, "0")
            placeholder(2, "false")
          }
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        file("TaskFile1.kt", """
          fun foo(): String = ""
          fun bar(): Int = 0
          fun baz(): Boolean = false
        """)
        file("task.html")
      }
    }.assertEquals(rootDir)

    val task = course.lessons[0].taskList[0]
    val taskDir = task.getDir(project.courseDir)!!
    val openFiles = FileEditorManagerEx.getInstanceEx(project).openFiles
    openFiles.forEach { openFile ->
      assertTrue(VfsUtil.isAncestor(taskDir, openFile, true))
    }
    assertContainsElements(openFiles.toList(), task.getAllTestVFiles(project))
    assertContainsElements(openFiles.toList(), task.getDescriptionFile(project))
  }

}
