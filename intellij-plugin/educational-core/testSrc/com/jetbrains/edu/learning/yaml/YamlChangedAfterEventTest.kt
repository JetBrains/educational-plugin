package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.assertContentsEqual
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.testAction
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import org.junit.Test

class YamlChangedAfterEventTest : YamlTestCase() {
  @Test
  fun `test hyperskill framework lesson navigation with learner created file`() {
    val course = createHyperskillFrameworkCourse(isTemplateBased = false)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, project.courseDir, "lesson1/task/userFile.txt", "user file")
      task1.openTaskFileInEditor("file1.txt")
      testAction(NextTaskAction.ACTION_ID)
    }

    val expectedConfig = """
      |type: edu
      |files:
      |- name: file1.txt
      |  visible: true
      |  learner_created: true
      |- name: userFile.txt
      |  visible: true
      |  learner_created: true
      |status: Unchecked
      |record: -1
      |""".trimMargin()

    val task2 = course.findTask("lesson1", "task2")

    checkConfig(course.findTask("lesson1", "task2"), expectedConfig)
    assertContentsEqual(task2, "file1.txt", "task 1")
    assertContentsEqual(task2, "userFile.txt", "user file")
  }

  private fun checkConfig(item: StudyItem, expectedConfig: String) {
    UIUtil.dispatchAllInvocationEvents()

    val configFile = item.getConfigDir(project).findChild(item.configFileName)
                     ?: error("No config file for item: ${item::class.simpleName} ${item.name}")

    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    assertEquals(expectedConfig, document.text)
  }

  @Suppress("SameParameterValue")
  private fun createHyperskillFrameworkCourse(isTemplateBased: Boolean): HyperskillCourse {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask {
          taskFile("file1.txt", "task 1")
        }
        eduTask {
          taskFile("file2.txt", "task2")
        }
        eduTask {
          taskFile("file3.txt", "task3")
        }
      }
    } as HyperskillCourse

    val hyperskillProject = HyperskillProject()
    hyperskillProject.isTemplateBased = isTemplateBased

    course.hyperskillProject = hyperskillProject
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))

    createConfigFiles(project)

    return course
  }
}