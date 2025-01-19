package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.jetbrains.edu.learning.messages.EduCoreBundle

// BACKCOMPAT: 2024.2 inline it
fun Row.courseLocationField(project: Project): Cell<TextFieldWithBrowseButton> {
  val fileChooserDescriptor = FileChooserDescriptorFactory
    .createSingleFolderDescriptor()
    .withTitle(EduCoreBundle.message("course.creator.create.archive.location.title"))
  return textFieldWithBrowseButton(fileChooserDescriptor, project)
}