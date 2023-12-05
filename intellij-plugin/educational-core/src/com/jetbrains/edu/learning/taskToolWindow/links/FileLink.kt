package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class FileLink(link: String) : TaskDescriptionLink<VirtualFile>(link, EduCounterUsageCollector.LinkType.FILE) {
  override fun resolve(project: Project): VirtualFile? {
    return project.courseDir.findFileByRelativePath(linkPath)
  }

  override fun open(project: Project, file: VirtualFile) {
    runInEdt {
      FileEditorManager.getInstance(project).openFile(file, false)
    }
  }
}
