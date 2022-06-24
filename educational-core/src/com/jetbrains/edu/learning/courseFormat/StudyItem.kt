package com.jetbrains.edu.learning.courseFormat

import java.util.*

/**
 * Base class for all items in course: section, lesson, task
 *
 * For each base type of study item (`Course`, `Section`, `Lesson`, `Task`)
 * there is the corresponding element in `StudyItemType` enum.
 *
 * @see Section
 * @see Lesson
 * @see FrameworkLesson
 * @see com.jetbrains.edu.learning.courseFormat.tasks.Task
 *
 * @see com.jetbrains.edu.coursecreator.StudyItemType
 */
abstract class StudyItem() {
  // from 1 to number of items
  var index: Int = -1
  var name: String = ""
  var updateDate: Date = Date(0)
  var id: Int = 0 // id on remote resource (Stepik, CheckIO, Codeforces)

  @Transient
  private var _parent: ItemContainer? = null

  var parent: ItemContainer
    @com.intellij.util.xmlb.annotations.Transient
    get() = _parent ?: error("Parent is null for StudyItem $name")
    @com.intellij.util.xmlb.annotations.Transient
    set(value) {
      _parent = value
    }

  var contentTags: List<String> = listOf()

  abstract val course: Course
  abstract val itemType: String     // used in json/yaml serialization/deserialization

  // Non unique lesson/task/section names can be received from stepik. In this case unique directory name is generated,
  // but original non unique name is displayed
  @get:Deprecated("Should be used only for deserialization. Use {@link StudyItem#getPresentableName()} instead")
  var customPresentableName: String? = null
  val presentableName: String
    get() = customPresentableName ?: name

  constructor(name: String) : this() {
    this.name = name
  }

  abstract fun init(parentItem: ItemContainer, isRestarted: Boolean)

  fun generateId() {
    if (id == 0) {
      id = System.identityHashCode(this)
    }
  }
}
