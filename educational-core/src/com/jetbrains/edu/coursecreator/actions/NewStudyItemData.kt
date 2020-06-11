package com.jetbrains.edu.coursecreator.actions

import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.messages.BUNDLE
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.PropertyKey
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

enum class StudyItemType(
  @PropertyKey(resourceBundle = BUNDLE) private val nameKey: String,
  @PropertyKey(resourceBundle = BUNDLE) private val titleNameKey: String
) {
  COURSE("study.item.course", "study.item.course.title"),
  SECTION("study.item.section", "study.item.section.title"),
  LESSON("study.item.lesson", "study.item.lesson.title"),
  FRAMEWORK_LESSON("study.item.framework.lesson", "study.item.framework.lesson.title"),
  TASK("study.item.task", "study.item.task.title");

  val presentableName: String get() = EduCoreBundle.message(nameKey)

  val presentableTitleName: String get() = EduCoreBundle.message(titleNameKey)
}
