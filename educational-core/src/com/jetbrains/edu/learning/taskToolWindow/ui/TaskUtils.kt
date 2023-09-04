@file:JvmName("TaskUtils")
package com.jetbrains.edu.learning.taskToolWindow.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.IMG_TAG
import com.jetbrains.edu.learning.taskToolWindow.SCRIPT_TAG
import com.jetbrains.edu.learning.taskToolWindow.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskToolWindow.ui.styleManagers.StyleManager
import org.apache.commons.lang.text.StrSubstitutor
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.io.File

fun htmlWithResources(
  project: Project,
  content: String,
  task: Task? = project.getCurrentTask()
): String {
  val templateText = loadText("/style/template.html.ft")
  val textWithResources = StrSubstitutor(StyleManager.resources(content)).replace(templateText) ?: "Cannot load task text"
  return absolutizePaths(project, textWithResources, task)
}

fun loadText(filePath: String): String? {
  val stream = try {
    object {}.javaClass.getResourceAsStream(filePath)
  } catch (e: NullPointerException) {
    return null
  }

  return stream.use {
    it.bufferedReader().readText()
  }
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

fun getSortingShortcutHTML(
  upContent: String,
  downContent: String,
  attributes: String = "",
): String {
  fun String.wrap() = "<span class='shortcut-description' $attributes>$this</span>"

  val x1 = upContent.wrap()
  val y1 = downContent.wrap()
  val shift = "<label class='textShortcut'>Shift</label>".wrap()

  val selectShortcut = "$x1 / $y1"
  val reorderShortcut = "$shift $x1 / $y1"

  return EduCoreBundle.message("hyperskill.sorting.tasks.shortcut.description", selectShortcut, reorderShortcut)
}