package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test

class YamlSynchronizationTest : YamlTestCase() {

  override fun createCourse() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, description = "test") {
      section {
        lesson {
          eduTask()
        }
      }
    }
  }

  @Test
  fun `test invalid config file`() {
    project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, false)
    createConfigFiles(project)

    // make task config invalid
    val section = StudyTaskManager.getInstance(project).course!!.sections.first()
    val lesson = section.lessons.first()
    val task = lesson.taskList.first()
    val taskConfig = task.getDir(project.courseDir)?.findChild(YamlConfigSettings.TASK_CONFIG)!!
    runWriteAction { VfsUtil.saveText(taskConfig, "invalid text") }
    StudyTaskManager.getInstance(project).course = null

    // check loaded task is null
    val loadedSection = StudyTaskManager.getInstance(project).course?.sections?.first()
    val loadedLesson = loadedSection?.lessons?.first()

    val loadedTask = loadedLesson?.taskList?.firstOrNull()
    assertNull(loadedTask)

    // check that config file wasn't overwritten
    val taskDir = loadedLesson!!.getDir(project.courseDir)?.findChild("task1")
    val loadedTaskConfig = taskDir?.findChild(YamlConfigSettings.TASK_CONFIG)!!
    val loadedTaskConfigDocument = FileDocumentManager.getInstance().getDocument(loadedTaskConfig)!!
    assertEquals("invalid text", loadedTaskConfigDocument.text)
  }
}