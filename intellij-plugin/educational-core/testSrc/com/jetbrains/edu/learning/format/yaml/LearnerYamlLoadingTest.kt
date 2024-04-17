package com.jetbrains.edu.learning.format.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.yaml.YamlDeepLoader
import com.jetbrains.rd.util.first
import org.junit.Test

class LearnerYamlLoadingTest : EduTestCase() {

  @Test
  fun `test non-existing files removed from config`() {
    val removedFileName = "file2.txt"
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("file1.txt")
          taskFile(removedFileName)
        }
      }
    }

    createConfigFiles(project)
    runWriteAction {
      findFileInTask(0, 0, removedFileName).delete(this)
    }
    loadFromYaml()
    assertEquals(1, findTask(0, 0).taskFiles.size)
  }

  //see EDU-2794
  @Test
  fun `test files not removed from framework lesson next tasks`() {
    courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("file1.txt")
        }
        eduTask {
          taskFile("file1.txt")
          taskFile("file2.txt")
        }
      }
    }

    createConfigFiles(project)
    loadFromYaml()
    assertEquals(2, findTask(0, 1).taskFiles.size)
  }

  @Test
  fun `test non-existing files removed from framework lesson current task`() {
    val removedFileName = "file2.txt"
    courseWithFiles {
      frameworkLesson {
        eduTask {
          taskFile("file1.txt")
        }
        eduTask {
          taskFile("file1.txt")
          taskFile(removedFileName)
        }
      }
    }
    val task1 = findTask(0, 0)
    val task2 = findTask(0, 1)

    NavigationUtils.navigateToTask(project, task2, task1)
    createConfigFiles(project)
    runWriteAction {
      findFileInTask(0, 1, removedFileName).delete(this)
    }

    loadFromYaml()
    assertEquals(1, findTask(0, 0).taskFiles.size)
  }

  @Test
  fun `test editable file`() {
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("file1.txt")
        }
      }
    }
    createConfigFiles(project)

    loadFromYaml()
    val task = findTask(0, 0)
    UIUtil.dispatchAllInvocationEvents()
    val taskFileVF = task.taskFiles.first().value.getVirtualFile(project)!!
    val course = StudyTaskManager.getInstance(project).course!!
    assertTrue(course.isEditableFile(taskFileVF.path))
  }

  @Test
  fun `test non editable file`() {
    val nonEditableFileName = "non-editable-file.txt"
    courseWithFiles {
      lesson {
        eduTask {
          taskFile("file1.txt")
          taskFile(nonEditableFileName, editable = false)
        }
      }
    }
    createConfigFiles(project)

    loadFromYaml()
    val task = findTask(0, 0)
    UIUtil.dispatchAllInvocationEvents()
    val taskFileVF = task.taskFiles[nonEditableFileName]!!.getVirtualFile(project)!!
    val course = StudyTaskManager.getInstance(project).course!!
    assertFalse(course.isEditableFile(taskFileVF.path))
  }

  private fun loadFromYaml() {
    val loadedCourse = YamlDeepLoader.loadCourse(project)
    StudyTaskManager.getInstance(project).course = loadedCourse
  }
}