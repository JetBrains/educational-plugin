package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeLesson

/**
 * To introduce new lesson type it's required to:
 * - Extend Lesson class
 * - Handle yaml deserialization [com.jetbrains.edu.learning.yaml.YamlDeserializer.deserializeLesson]
 */
open class Lesson : ItemContainer() {

  // TODO: move to stepik
  @Transient
  var steps: List<Int> = listOf()
  @Transient
  var isPublic = false
  var unitId = 0

  override fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean) {
    require(parentItem is LessonContainer) { "Parent for lesson $name should be either course or section" }
    parent = parentItem

    for ((i, task) in taskList.withIndex()) {
      task.index = i + 1
      task.init(course, this, isRestarted)
    }
  }

  /**
   * Returns tasks copy. Dedicated methods should be used to modify list of lesson items ([addTask], [removeTask])
   */
  val taskList: List<Task>
    get() = items.filterIsInstance<Task>()

  override val course: Course
    get() = parent.course
  val section: Section?
    get() = parent as? Section

  override val itemType: String = EduNames.LESSON

  fun addTask(task: Task) {
    addItem(task)
  }

  fun addTask(index: Int, task: Task) {
    addItem(index, task)
  }

  fun removeTask(task: Task) {
    removeItem(task)
  }

  fun getTask(name: String): Task? {
    return getItem(name) as? Task
  }

  fun getTask(id: Int): Task? {
    return taskList.firstOrNull { it.id == id }
  }

  val container: LessonContainer
    get() = section ?: course

  override fun getDir(baseDir: VirtualFile): VirtualFile? {
    return if (parent is Section) {
      val sectionDir = baseDir.findChild(parent.name) ?: error("Section dir for lesson not found")
      sectionDir.findChild(name)
    }
    else {
      baseDir.findChild(name)
    }
  }

  fun visitTasks(visit: (Task) -> Unit) {
    taskList.forEach(visit)
  }
}
