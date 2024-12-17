package com.jetbrains.edu.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.`in`
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.deserializeItemProcessingErrors
import org.junit.Test

class VirtualFileListenerTest : VirtualFileListenerTestBase() {

  override val courseMode: CourseMode = CourseMode.STUDENT

  override fun createListener(project: Project): EduVirtualFileListener = UserCreatedFileListener(project)

  @Test
  fun `test add task file`() {
    val filePath = "src/taskFile.txt"
    val studentMapper = YamlMapper.studentMapper()
    doAddFileTest(filePath) { task ->
      listOf((filePath `in` task).withAdditionalCheck { taskFile ->
        assertEquals(true, taskFile.isLearnerCreated)
        val taskConfigFile = task.getDir(project.courseDir)?.findChild(task.configFileName) ?: error("Failed to find config file")
        // after task files is created, changes are saved to config in `invokeLater`
        // we want to check config after it happened, means this event is dispatched
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        val item = deserializeItemProcessingErrors(taskConfigFile, project, mapper = studentMapper) as EduTask
        val deserializedTaskFile = item.getTaskFile(taskFile.name)
                                   ?: error("Learner config file doesn't contain `${taskFile.name}` task file")
        assertEquals(true, deserializedTaskFile.isLearnerCreated)
      })
    }
  }

  @Test
  fun `FS operations do not affect the list of additional files in the student mode`() {
    val initialAdditionalFiles = listOf("a.txt", "dir/b.txt", "lesson2/b.txt")

    doTestAdditionalFilesAfterFSActions(initialAdditionalFiles, initialAdditionalFiles) {
      createFile("file.txt")
      copyFile("a.txt", ".", copyName = "a_copy.txt")
      renameFile("a.txt", "a_renamed.txt")
      moveFile("dir", "lesson2")
      deleteFile("lesson2/b.txt")
    }
  }
}
