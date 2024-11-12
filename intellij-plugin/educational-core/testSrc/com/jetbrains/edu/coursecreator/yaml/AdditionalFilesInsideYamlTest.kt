package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.coursecreator.AdditionalFilesUtils.collectAdditionalFiles
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import org.junit.Test
import kotlin.test.assertContentEquals

class AdditionalFilesInsideYamlTest : EduTestCase() {

  @Test
  fun `collecting additional files does not need the course object`() {
    courseWithFiles(createYamlConfigs = true) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt")
        }
      }
      additionalFile("file1.txt")
      additionalFile("lesson1/file2.txt")
      additionalFile("lesson1/task1/file3.txt") // not an additional file, because is inside the folder of the "task1" task
    }

    val course = project.course!!
    StudyTaskManager.getInstance(project).course = EduCourse() // this course has no tasks

    val additionalFiles = collectAdditionalFiles(course.configurator, project, detectTaskFoldersByContents = true)
      .sortedBy { it.name }

    assertContentEquals(listOf("file1.txt", "lesson1/file2.txt"), additionalFiles.map { it.name })
  }

}