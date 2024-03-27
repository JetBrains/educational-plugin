package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.components.*
import com.intellij.openapi.components.State
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.getPathInCourse
import com.jetbrains.edu.learning.courseFormat.ext.getRelativePath
import com.jetbrains.edu.learning.courseFormat.ext.visitTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.VisibleForTesting

/*
 * A service that is used as the persistent storage for task records in the framework lesson the for course creator,
 * instead of storing them in yaml file.
 *
 * Record of the task - key in the storage framework for the task.
 * It is needed to get information about the previous state of the task, which is stored in the framework storage.
 * For learner, we store the record in yaml, but this option is not suitable for the CC, since we do not want to clutter a yaml.
 * Therefore, the record is stored for the task creator in this service.
 *
 * Currently, the key is the path to the task folder
 * TODO(use id of the task as a key instead of the path to the task)
 */
@Service(Service.Level.PROJECT)
@State(name = "CCFrameworkLessonRecordStorage", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CCFrameworkLessonRecordStorage : SimplePersistentStateComponent<CCFrameworkLessonRecordStorage.State>(State()) {
  fun getRecord(task: Task): Int {
    if (task.parent !is FrameworkLesson) return -1
    val key = task.getPathInCourse()
    return state.taskRecords[key] ?: -1
  }

  fun updateRecord(task: Task, newRecord: Int) {
    if (task.parent !is FrameworkLesson) return
    val key = task.getPathInCourse()
    state.updateTaskRecord(key, newRecord)
    task.record = newRecord
  }

  fun removeRecords(itemContainer: ItemContainer) = itemContainer.visitTasks { removeRecord(it) }

  fun removeRecord(task: Task) {
    if (task.parent !is FrameworkLesson) return
    val key = task.getPathInCourse()
    state.removeTaskRecord(key)
  }

  fun migrateRecords(studyItem: StudyItem, newName: String) {
    if (studyItem is Course) return
    val oldName = studyItem.name

    val prefixPath = studyItem.parent.getPathInCourse()

    studyItem.visitTasks { task ->
      if (task.parent !is FrameworkLesson) return@visitTasks

      val suffixPath = task.getRelativePath(studyItem)

      val oldPath = listOf(prefixPath, oldName, suffixPath).filter { it.isNotEmpty() }.joinToString(VfsUtilCore.VFS_SEPARATOR)
      val newPath = listOf(prefixPath, newName, suffixPath).filter { it.isNotEmpty() }.joinToString(VfsUtilCore.VFS_SEPARATOR)

      state.migrateTaskRecord(oldPath, newPath)
    }
  }

  @VisibleForTesting
  fun getRecord(path: String): Int? {
    return state.taskRecords[path]
  }

  @VisibleForTesting
  fun reset() {
    state.taskRecords.clear()
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    val taskRecords: MutableMap<String, Int> by map()

    fun updateTaskRecord(key: String, newRecord: Int) {
      taskRecords[key] = newRecord
      incrementModificationCount()
    }

    fun removeTaskRecord(key: String) {
      taskRecords.remove(key)
      incrementModificationCount()
    }

    fun migrateTaskRecord(oldKey: String, newKey: String) {
      val record = taskRecords[oldKey] ?: return
      taskRecords.remove(oldKey)
      taskRecords[newKey] = record
      incrementModificationCount()
    }
  }

  companion object {
    fun getInstance(project: Project): CCFrameworkLessonRecordStorage = project.service()
  }
}