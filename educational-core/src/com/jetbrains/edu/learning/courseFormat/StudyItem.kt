package com.jetbrains.edu.learning.courseFormat

import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
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
  val dataHolder: UserDataHolder = UserDataHolderBase()
  var contentTags: List<String> = listOf()

  abstract val course: Course
  abstract val parent: ItemContainer
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

  abstract fun init(course: Course?, parentItem: StudyItem?, isRestarted: Boolean)

  abstract fun getDir(baseDir: VirtualFile): VirtualFile?

  fun generateId() {
    if (id == 0) {
      id = System.identityHashCode(this)
    }
  }

  fun getPathInCourse(): String {
    val parents = mutableListOf<String>()
    var currentParent = parent
    while (currentParent !is Course) {
      parents.add(currentParent.name)
      currentParent = currentParent.parent
    }
    parents.reverse()
    if (parents.isEmpty()) return name
    parents.add(name)
    return parents.joinToString(VfsUtilCore.VFS_SEPARATOR)
  }
}