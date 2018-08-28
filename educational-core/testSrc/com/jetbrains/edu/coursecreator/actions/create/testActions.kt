package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.actions.CCCreateLesson
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.NewStudyItemInfo
import com.jetbrains.edu.coursecreator.actions.sections.CCCreateSection
import com.jetbrains.edu.learning.courseFormat.StudyItem

class CCTestCreateTask(private val myName: String, private val myIndex: Int) : CCCreateTask() {
  override fun showCreateStudyItemDialogWithPosition(thresholdItem: StudyItem,
                                                     project: Project,
                                                     sourceDirectory: VirtualFile): NewStudyItemInfo? = NewStudyItemInfo(myName, myIndex)
}

class CCTestCreateLesson(private val myName: String, private val myIndex: Int) : CCCreateLesson() {
  override fun showCreateStudyItemDialogWithPosition(thresholdItem: StudyItem,
                                                     project: Project,
                                                     sourceDirectory: VirtualFile): NewStudyItemInfo? = NewStudyItemInfo(myName, myIndex)
}

class CCTestCreateSection(private val myName: String, private val myIndex: Int) : CCCreateSection() {
  override fun showCreateStudyItemDialogWithPosition(thresholdItem: StudyItem,
                                                     project: Project,
                                                     sourceDirectory: VirtualFile): NewStudyItemInfo? = NewStudyItemInfo(myName, myIndex)
}
