package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.StudyItemType
import com.jetbrains.edu.learning.courseFormat.StudyItem
import org.jetbrains.annotations.Nls
import javax.swing.Icon

class NewStudyItemInfo(
  val name: String,
  val index: Int,
  val producer: () -> StudyItem
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
  val baseIndex: Int,
  val studyItemVariants: List<StudyItemVariant>
)

data class StudyItemVariant(
  @Nls(capitalization = Nls.Capitalization.Title)
  val type: String,
  @Nls(capitalization = Nls.Capitalization.Sentence)
  val description: String,
  val icon: Icon,
  val producer: () -> StudyItem
)
