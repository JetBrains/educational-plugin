package com.jetbrains.edu.coursecreator

import com.intellij.openapi.keymap.KeymapUtil
import com.jetbrains.edu.coursecreator.StudyItemType.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
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
    COURSE -> EduCoreBundle.message("study.item.course")
    SECTION -> EduCoreBundle.message("study.item.section")
    LESSON -> EduCoreBundle.message("study.item.lesson")
    TASK -> EduCoreBundle.message("study.item.task")
  }

val StudyItemType.presentableTitleName: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreBundle.message("study.item.course.title")
    SECTION -> EduCoreBundle.message("study.item.section.title")
    LESSON -> EduCoreBundle.message("study.item.lesson.title")
    TASK -> EduCoreBundle.message("study.item.task.title")
  }

val StudyItemType.createItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE -> EduCoreBundle.message("study.item.create.course")
    SECTION -> EduCoreBundle.message("study.item.create.section")
    LESSON -> EduCoreBundle.message("study.item.create.lesson")
    TASK -> EduCoreBundle.message("study.item.create.task")
  }

val StudyItemType.createItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreBundle.message("study.item.create.course.title")
    SECTION -> EduCoreBundle.message("study.item.create.section.title")
    LESSON -> EduCoreBundle.message("study.item.create.lesson.title")
    TASK -> EduCoreBundle.message("study.item.create.task.title")
  }

val StudyItemType.newItemTitleMessage: String
  @Nls(capitalization = Nls.Capitalization.Title)
  get() = when (this) {
    COURSE -> EduCoreBundle.message("study.item.new.course.title")
    SECTION -> EduCoreBundle.message("study.item.new.section.title")
    LESSON -> EduCoreBundle.message("study.item.new.lesson.title")
    TASK -> EduCoreBundle.message("study.item.new.task.title")
  }

val StudyItemType.pressEnterToCreateItemMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() {
    val enter = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
    return when (this) {
      COURSE -> EduCoreBundle.message("action.new.study.item.hint.course", enter)
      SECTION -> EduCoreBundle.message("action.new.study.item.hint.section", enter)
      LESSON -> EduCoreBundle.message("action.new.study.item.hint.lesson", enter)
      TASK -> EduCoreBundle.message("action.new.study.item.hint.task", enter)
    }
  }

val StudyItemType.selectItemTypeMessage: String
  @Nls(capitalization = Nls.Capitalization.Sentence)
  get() = when (this) {
    COURSE -> EduCoreBundle.message("action.new.study.item.select.type.course")
    SECTION -> EduCoreBundle.message("action.new.study.item.select.type.section")
    LESSON -> EduCoreBundle.message("action.new.study.item.select.type.lesson")
    TASK -> EduCoreBundle.message("action.new.study.item.select.type.task")
  }

@Nls(capitalization = Nls.Capitalization.Sentence)
fun StudyItemType.failedToFindItemMessage(@NonNls itemName: String): String = when (this) {
  COURSE -> EduCoreBundle.message("error.yaml.failed.to.find.course", itemName)
  SECTION -> EduCoreBundle.message("error.yaml.failed.to.find.section", itemName)
  LESSON -> EduCoreBundle.message("error.yaml.failed.to.find.lesson", itemName)
  TASK -> EduCoreBundle.message("error.yaml.failed.to.find.task", itemName)
}