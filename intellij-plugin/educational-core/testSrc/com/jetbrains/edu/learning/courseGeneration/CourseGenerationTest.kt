package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.EduStartupActivity
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getAllTestVFiles
import com.jetbrains.edu.learning.courseFormat.ext.getDescriptionFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class CourseGenerationTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  @Test
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

  @Test
  fun `test substitute placeholder answers in CC mode`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
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
        file("task.md")
      }
    }.assertEquals(rootDir)
  }

  @Test
  fun `test opened files in CC mode`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
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
        file("task.md")
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

  @Test
  fun `test course preview not added to course storage`() {
    val coursePreview = (course {
      lesson("lesson1") {
        eduTask("task1") {
        }
      }
    } as EduCourse).apply {
      isPreview = true
    }

    createCourseStructure(coursePreview)

    // wa have to call it here as it is called itself at time when course isn't set to StudyTaskManager
    runBlocking { EduStartupActivity().execute(myProject) }

    assertFalse("Course `${coursePreview.name}` shouldn't be added to course storage", CoursesStorage.getInstance().hasCourse(coursePreview))
  }
}
