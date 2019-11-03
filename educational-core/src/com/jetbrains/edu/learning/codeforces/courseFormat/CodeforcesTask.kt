package com.jetbrains.edu.learning.codeforces.courseFormat

import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.sourceDir
import com.jetbrains.edu.learning.courseFormat.ext.testDirs
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import org.jsoup.nodes.Element

class CodeforcesTask : Task {
  @Suppress("unused") //used for deserialization
  constructor()

  constructor(htmlElement: Element, lesson: Lesson) {
    this.lesson = lesson

    name = htmlElement.select("div.header").select("div.title").text()

    // TODO if tests to check won't be implemented - uncomment next line
//    htmlElement.selectFirst(".sample-tests").remove()
    descriptionText = htmlElement.outerHtml()
      // This replacement is needed for proper MathJax visualization
      .replace("$$$", "$")
      // Replace picture src property if it starts with slash but not codeforces URL
      .replace("src=\"/", "src=\"${CodeforcesNames.CODEFORCES_URL}/")

    feedbackLink = FeedbackLink(
      "${(course as CodeforcesCourse).contestUrl}/problem/${name.substringBefore(".")}?locale=${course.languageCode}")

    createTaskFile()
    createTestFile()
  }

  override fun getItemType(): String = CODEFORCES_TASK_TYPE

  private fun createTaskFile() {
    val taskTemplateName = lesson.course.configurator?.courseBuilder?.taskTemplateName
    val name = taskTemplateName ?: "Task"
    val text = if (taskTemplateName != null) GeneratorUtils.getInternalTemplateText(taskTemplateName) else "type task text here"
    addTaskFile(TaskFile(GeneratorUtils.joinPaths(sourceDir, name), text))
  }

  private fun createTestFile() {
    val testTemplateName = lesson.course.configurator?.courseBuilder?.testTemplateName
    val name = testTemplateName ?: "Test"
    val text = if (testTemplateName != null) GeneratorUtils.getInternalTemplateText(testTemplateName) else "type test text here"
    addTaskFile(TaskFile(GeneratorUtils.joinPaths(testDirs[0], name), text).apply { isVisible = true })
  }
}