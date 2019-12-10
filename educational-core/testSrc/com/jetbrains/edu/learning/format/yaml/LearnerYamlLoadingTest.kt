package com.jetbrains.edu.learning.format.yaml

import com.intellij.openapi.application.runWriteAction
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.yaml.YamlDeepLoader

class LearnerYamlLoadingTest : EduTestCase() {

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

  private fun loadFromYaml() {
    val loadedCourse = YamlDeepLoader.loadCourse(project)
    StudyTaskManager.getInstance(project).course = loadedCourse
  }
}