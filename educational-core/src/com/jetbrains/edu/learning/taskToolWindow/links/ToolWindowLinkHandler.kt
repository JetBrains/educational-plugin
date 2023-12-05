package com.jetbrains.edu.learning.taskToolWindow.links

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser


open class ToolWindowLinkHandler(val project: Project) {

  protected fun processExternalLink(url: String) = EduBrowser.getInstance().browse(url)

  /**
   * @return false to continue (for example open external link at task description), otherwise true
   */
  open fun process(url: String, referUrl: String? = null): Boolean {
    val link = TaskDescriptionLink.fromUrl(url)
    if (link != null) {
      link.open(project)
    }
    else {
      processExternalLink(url)
    }
    return true
  }
}
