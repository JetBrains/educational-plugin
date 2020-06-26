package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.yaml.createConfigFiles
import com.jetbrains.edu.learning.actions.NextTaskAction
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillProject
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class YamlChangedAfterEventTest : YamlTestCase() {
  fun `test hyperskill framework lesson navigation with learner created file`() {
    val course = createHyperskillFrameworkCourse(isTemplateBased = false)

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      GeneratorUtils.createChildFile(course.getDir(project), "lesson1/task/userFile.txt", "user file")
      task1.openTaskFileInEditor("file1.txt")
      task1.status = CheckStatus.Solved
      myFixture.testAction(NextTaskAction())
    }

    val expectedConfig = """
      |type: edu
      |files:
      |- name: file1.txt
      |  visible: true
      |  text: task 1
      |  learner_created: true
      |- name: userFile.txt
      |  visible: true
      |  text: user file
      |  learner_created: true
      |status: Unchecked
      |record: -1
      |""".trimMargin()

    checkConfig(course.findTask("lesson1", "task2"), expectedConfig)
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

    createConfigFiles(project)

    return course
  }
}