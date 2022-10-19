package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.taskDescription.IMG_TAG
import com.jetbrains.edu.learning.taskDescription.SCRIPT_TAG
import com.jetbrains.edu.learning.taskDescription.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskDescription.ui.loadText
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import org.apache.commons.lang.text.StrSubstitutor
import org.jetbrains.annotations.VisibleForTesting
import org.jsoup.Jsoup
import java.io.File

object ResourceWrapper : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String = htmlWithResources(context.project, html, context.task)
}

@VisibleForTesting
fun htmlWithResources(
  project: Project,
  content: String,
  task: Task? = EduUtils.getCurrentTask(project)
): String {
  val templateText = loadText("/style/template.html.ft")
  val textWithResources = StrSubstitutor(StyleManager.resources(content)).replace(templateText) ?: "Cannot load task text"
  return absolutizePaths(project, textWithResources, task)
}

private fun absolutizePaths(project: Project, content: String, task: Task?): String {
  val taskDir = task?.getDir(project.courseDir) ?: return content

  val document = Jsoup.parse(content)
  val imageElements = document.getElementsByTag(IMG_TAG)
  val scriptElements = document.getElementsByTag(SCRIPT_TAG)
  for (element in scriptElements + imageElements) {
    val src = element.attr(SRC_ATTRIBUTE)
    if (src.isNotEmpty() && !BrowserUtil.isAbsoluteURL(src)) {
      val file = File(src)
      val absolutePath = File(taskDir.path, file.path).toURI().toString()
      element.attr(SRC_ATTRIBUTE, absolutePath)
    }
  }
  return document.outerHtml()
}