package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.configFileName

class VirtualFileListenerTest : VirtualFileListenerTestBase() {

  override val courseMode: String = EduNames.STUDY

  override fun createListener(project: Project): EduVirtualFileListener = UserCreatedFileListener(project)

  fun `test add task file`() {
    val filePath = "src/taskFile.txt"
    doAddFileTest(filePath) { task ->
      listOf((filePath `in` task).withAdditionalCheck { taskFile ->
        assertEquals(true, taskFile.isLearnerCreated)
        val taskConfigFile = task.getDir(project)?.findChild(task.configFileName) ?: error("Failed to find config file")
        // after task files is created, changes are saved to config in `invokeLater`
        // we want to check config after it happened, means this event is dispatched
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        val item = YamlDeserializer.deserializeItem(project, taskConfigFile, YamlFormatSynchronizer.STUDENT_MAPPER) as EduTask
        val deserializedTaskFile = item.getTaskFile(taskFile.name) ?: error("Learner config file doesn't contain `${taskFile.name}` task file")
        assertEquals(true, deserializedTaskFile.isLearnerCreated)
      })
    }
  }
}
