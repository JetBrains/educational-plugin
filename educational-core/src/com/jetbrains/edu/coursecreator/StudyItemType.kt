package com.jetbrains.edu.coursecreator

import com.intellij.openapi.keymap.KeymapUtil
import com.jetbrains.edu.learning.messages.EduCoreStudyItemBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

sealed class StudyItemType {
  abstract val presentableName: String
    @Nls get

  abstract val presentableTitleName: String
    @Nls(capitalization = Nls.Capitalization.Title) get

  abstract val createItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence) get

  abstract val createItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title) get

  abstract val newItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title) get

  abstract val selectItemTypeMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence) get

  val pressEnterToCreateItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() {
      val enter = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
      return pressEnterToCreateItemMessageImpl(enter)
    }

  @Nls(capitalization = Nls.Capitalization.Sentence)
  abstract fun failedToFindItemMessage(@NonNls itemName: String): String

  @Nls(capitalization = Nls.Capitalization.Sentence)
  protected abstract fun pressEnterToCreateItemMessageImpl(enter: String): String
}

object CourseType : StudyItemType() {
  override val presentableName: String
    @Nls get() = EduCoreStudyItemBundle.message("item.course")

  override val presentableTitleName: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("item.course.title")

  override val createItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("create.course")

  override val createItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("create.course.title")

  override val newItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("new.course.title")

  override val selectItemTypeMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("select.type.course")

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun failedToFindItemMessage(itemName: String): String =
    EduCoreStudyItemBundle.message("failed.to.find.course", itemName)

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun pressEnterToCreateItemMessageImpl(enter: String): String =
    EduCoreStudyItemBundle.message("hint.press.enter.to.create.course", enter)
}

object SectionType : StudyItemType() {
  override val presentableName: String
    @Nls get() = EduCoreStudyItemBundle.message("item.section")

  override val presentableTitleName: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("item.section.title")

  override val createItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("create.section")

  override val createItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("create.section.title")

  override val newItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("new.section.title")

  override val selectItemTypeMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("select.type.section")

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun failedToFindItemMessage(itemName: String): String =
    EduCoreStudyItemBundle.message("failed.to.find.section", itemName)

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun pressEnterToCreateItemMessageImpl(enter: String): String =
    EduCoreStudyItemBundle.message("hint.press.enter.to.create.section", enter)
}

object LessonType : StudyItemType() {
  override val presentableName: String
    @Nls get() = EduCoreStudyItemBundle.message("item.lesson")

  override val presentableTitleName: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("item.lesson.title")

  override val createItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("create.lesson")

  override val createItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("create.lesson.title")

  override val newItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("new.lesson.title")

  override val selectItemTypeMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("select.type.lesson")

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun failedToFindItemMessage(itemName: String): String =
    EduCoreStudyItemBundle.message("failed.to.find.lesson", itemName)

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun pressEnterToCreateItemMessageImpl(enter: String): String =
    EduCoreStudyItemBundle.message("hint.press.enter.to.create.lesson", enter)
}

object TaskType : StudyItemType() {
  override val presentableName: String
    @Nls get() = EduCoreStudyItemBundle.message("item.task")

  override val presentableTitleName: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("item.task.title")

  override val createItemMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("create.task")

  override val createItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("create.task.title")

  override val newItemTitleMessage: String
    @Nls(capitalization = Nls.Capitalization.Title)
    get() = EduCoreStudyItemBundle.message("new.task.title")

  override val selectItemTypeMessage: String
    @Nls(capitalization = Nls.Capitalization.Sentence)
    get() = EduCoreStudyItemBundle.message("select.type.task")

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun failedToFindItemMessage(itemName: String): String =
    EduCoreStudyItemBundle.message("failed.to.find.task", itemName)

  @Nls(capitalization = Nls.Capitalization.Sentence)
  override fun pressEnterToCreateItemMessageImpl(enter: String): String =
    EduCoreStudyItemBundle.message("hint.press.enter.to.create.task", enter)
}