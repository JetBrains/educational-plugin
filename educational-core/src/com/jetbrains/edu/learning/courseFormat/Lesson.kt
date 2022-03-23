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
  @Transient
  private var myCourse: Course? = null
  @Transient
  var section: Section? = null

  // TODO: move to stepik
  @Transient
  var steps: List<Int> = listOf()
  @Transient
  var isPublic = false
  var unitId = 0

  override fun init(course: Course?, section: StudyItem?, isRestarted: Boolean) {
    this.section = if (section is Section) section else null
    setCourse(course)

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

  override fun getCourse(): Course {
    return myCourse ?: error("Course is null for lesson $name")
  }

  override fun getParent(): StudyItem {
    return container
  }

  override fun getItemType(): String {
    return EduNames.LESSON
  }

  fun setCourse(course: Course?) {
    myCourse = course
  }

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
    return if (section == null) {
      baseDir.findChild(name)
    }
    else {
      val sectionDir = baseDir.findChild(section!!.name) ?: error("Section dir for lesson not found")
      sectionDir.findChild(name)
    }
  }

  fun visitTasks(visit: (Task) -> Unit) {
    taskList.forEach(visit)
  }
}
