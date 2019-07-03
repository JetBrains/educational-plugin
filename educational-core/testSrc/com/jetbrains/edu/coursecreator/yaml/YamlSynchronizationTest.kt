package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.edu.coursecreator.CCProjectComponent
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.StudyTaskManager

class YamlSynchronizationTest : YamlTestCase() {

  override fun createCourse() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson {
          eduTask()
        }
      }
    }
    course.description = "test"
  }

  fun `test invalid config file`() {
    project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, false)
    createConfigFiles()

    // make task config invalid
    val section = StudyTaskManager.getInstance(project).course!!.sections.first()
    val lesson = section.lessons.first()
    val task = lesson.taskList.first()
    val taskConfig = task.getDir(project)?.findChild(YamlFormatSettings.TASK_CONFIG)!!
    val document = FileDocumentManager.getInstance().getDocument(taskConfig)!!
    runWriteAction { document.setText("invalid text") }

    project.getComponent(CCProjectComponent::class.java).projectOpened()

    // check loaded task is null
    val loadedSection = StudyTaskManager.getInstance(project).course?.sections?.first()
    val loadedLesson = loadedSection?.lessons?.first()

    val loadedTask = loadedLesson?.taskList?.firstOrNull()
    assertNull(loadedTask)

    // check that config file wasn't overwritten
    val taskDir = loadedLesson!!.getDir(project)?.findChild("task1")
    val loadedTaskConfig = taskDir?.findChild(YamlFormatSettings.TASK_CONFIG)!!
    val loadedTaskConfigDocument = FileDocumentManager.getInstance().getDocument(loadedTaskConfig)!!
    assertEquals("invalid text", loadedTaskConfigDocument.text)
  }
}