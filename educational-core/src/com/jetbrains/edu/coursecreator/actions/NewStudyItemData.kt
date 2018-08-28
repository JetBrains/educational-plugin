package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.StudyItem

open class NewStudyItemInfo(
  val name: String,
  val index: Int
) : UserDataHolderBase()

/**
 * Model data for new study item creation UI
 */
data class NewStudyItemUiModel(
  val parent: StudyItem?,
  val parentDir: VirtualFile,
  val itemType: StudyItemType,
  val suggestedName: String,
  /**
   * Approximate index of new study item.
   *
   * If creating UI was called with parent item context (i.e. new task creation was called from lesson item),
   * it will be final index of new item.
   * If creating UI was called with sibling item context (i.e. new task creation was called from task item),
   * it will be index of this sibling item. The final index of new item will be chosen by user.
   */
  val baseIndex: Int
)

enum class StudyItemType(val presentableName: String) {
  COURSE(EduNames.COURSE),
  SECTION(EduNames.SECTION),
  LESSON(EduNames.LESSON),
  FRAMEWORK_LESSON(EduNames.FRAMEWORK_LESSON),
  TASK(EduNames.TASK);
}
