package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class HttpLink(link: String) : TaskDescriptionLink<String>(link) {
  override fun resolve(project: Project): String = link

  override fun open(project: Project, link: String) {
    EduBrowser.getInstance().browse(link)
  }
}
