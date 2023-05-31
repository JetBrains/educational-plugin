@file:JvmName("TaskUtils")
package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskDescription.IMG_TAG
import com.jetbrains.edu.learning.taskDescription.SCRIPT_TAG
import com.jetbrains.edu.learning.taskDescription.SRC_ATTRIBUTE
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import org.apache.commons.lang.text.StrSubstitutor
import org.jsoup.Jsoup
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

fun wrapHintTagsInsideHTML(text: String, wrapHint: (e: Element, number: String, title: String) -> String): String {
  val document = Jsoup.parse(text)
  val hints = document.getElementsByClass("hint")

  val hintDefaultTitle = EduCoreBundle.message("course.creator.yaml.hint.default.title")

  fun getHintTitle(hint: Element): String {
    val actualTitleValue = hint.attr("title")
    return if (actualTitleValue == "") hintDefaultTitle else actualTitleValue
  }

  // map hint title to count of the same titles
  val countHintTitles = hints.groupingBy { getHintTitle(it) }.eachCount()

  val indexByTitle = mutableMapOf<String, Int>()

  for (hint in hints) {
    val hintTitle = getHintTitle(hint)
    val index = indexByTitle.getOrDefault(hintTitle, 0)
    val hintsWithThisTitle = countHintTitles.getValue(hintTitle)
    indexByTitle[hintTitle] = indexByTitle.getOrDefault(hintTitle, 0) + 1

    val textualIndex = if (hintsWithThisTitle <= 1) "" else (index + 1).toString()
    val hintText = wrapHint(hint, textualIndex, hintTitle)

    // we remove the title attribute, because otherwise it may generate popup hints
    hint.removeAttr("title")

    hint.html(hintText)
  }

  return document.html()
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
