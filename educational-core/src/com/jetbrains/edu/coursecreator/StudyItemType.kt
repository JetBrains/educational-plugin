package com.jetbrains.edu.coursecreator

import com.intellij.openapi.keymap.KeymapUtil
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.messages.EduCoreStudyItemBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

enum class StudyItemType {
  COURSE,
  SECTION,
  LESSON,
  TASK;
}

val StudyItemType.presentableName: String
  @Nls get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("item.course")
    SECTION -> EduCoreStudyItemBundle.message("item.section")
    LESSON -> EduCoreStudyItemBundle.message("item.lesson")
    TASK -> EduCoreStudyItemBundle.message("item.task")
  }

val StudyItemType.presentableTitleName: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("item.course.title")
    SECTION -> EduCoreStudyItemBundle.message("item.section.title")
    LESSON -> EduCoreStudyItemBundle.message("item.lesson.title")
    TASK -> EduCoreStudyItemBundle.message("item.task.title")
  }

val StudyItemType.createItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("create.course")
    SECTION -> EduCoreStudyItemBundle.message("create.section")
    LESSON -> EduCoreStudyItemBundle.message("create.lesson")
    TASK -> EduCoreStudyItemBundle.message("create.task")
  }

val StudyItemType.createItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("create.course.title")
    SECTION -> EduCoreStudyItemBundle.message("create.section.title")
    LESSON -> EduCoreStudyItemBundle.message("create.lesson.title")
    TASK -> EduCoreStudyItemBundle.message("create.task.title")
  }

val StudyItemType.newItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("new.course.title")
    SECTION -> EduCoreStudyItemBundle.message("new.section.title")
    LESSON -> EduCoreStudyItemBundle.message("new.lesson.title")
    TASK -> EduCoreStudyItemBundle.message("new.task.title")
  }

val StudyItemType.pressEnterToCreateItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() {
    val enter = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    return when (this) {
      COURSE -> EduCoreStudyItemBundle.message("hint.press.enter.to.create.course", enter)
      SECTION -> EduCoreStudyItemBundle.message("hint.press.enter.to.create.section", enter)
      LESSON -> EduCoreStudyItemBundle.message("hint.press.enter.to.create.lesson", enter)
      TASK -> EduCoreStudyItemBundle.message("hint.press.enter.to.create.task", enter)
    }
  }

val StudyItemType.selectItemTypeMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE -> EduCoreStudyItemBundle.message("select.type.course")
    SECTION -> EduCoreStudyItemBundle.message("select.type.section")
    LESSON -> EduCoreStudyItemBundle.message("select.type.lesson")
    TASK -> EduCoreStudyItemBundle.message("select.type.task")
  }

@Nls(capitalization = Nls.Capitalization.Sentence)
fun StudyItemType.failedToFindItemMessage(@NonNls itemName: String): String = when (this) {
  COURSE -> EduCoreStudyItemBundle.message("failed.to.find.course", itemName)
  SECTION -> EduCoreStudyItemBundle.message("failed.to.find.section", itemName)
  LESSON -> EduCoreStudyItemBundle.message("failed.to.find.lesson", itemName)
  TASK -> EduCoreStudyItemBundle.message("failed.to.find.task", itemName)
}