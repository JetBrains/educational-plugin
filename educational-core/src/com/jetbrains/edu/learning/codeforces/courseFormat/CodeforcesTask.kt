package com.jetbrains.edu.learning.codeforces.courseFormat

import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TEST_DATA_FOLDER
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jsoup.nodes.Element

open class CodeforcesTask : Task() {
  open val inputFileName: String = "input.txt"
  open val outputFileName: String = "output.txt"

  override fun getItemType(): String = CODEFORCES_TASK_TYPE

  private fun addSampleTests(htmlElement: Element) {
    htmlElement.select("div.input").forEachIndexed { index, inputElement ->
      val testFolderName = (index + 1).toString()
      addTestTaskFile(inputElement, testFolderName, inputFileName)

      val outputElement = inputElement.nextElementSibling()
      addTestTaskFile(outputElement, testFolderName, outputFileName)
    }
  }

  private fun addTestTaskFile(htmlElement: Element, testFolderName: String, fileName: String) {
    val innerElement = htmlElement.select("pre")
    if (innerElement.isEmpty()) {
      error("Can't find HTML element with test data in ${htmlElement.text()}")
    }

    val path = join(listOf(TEST_DATA_FOLDER, testFolderName, fileName), VFS_SEPARATOR_CHAR.toString())
    addTaskFile(TaskFile(path, innerElement.first().text()))
  }

  companion object {
    fun create(htmlElement: Element, lesson: Lesson): CodeforcesTask {
      val isStandardIO = htmlElement.select("div.input-file, div.output-file").all { isStandardIOType(it) }

      val task = if (isStandardIO) {
        CodeforcesTask()
      }
      else {
        val inputFileName = htmlElement.selectFirst("div.input-file").ownText()
        val outputFileName = htmlElement.selectFirst("div.output-file").ownText()
        CodeforcesTaskWithFileIO(inputFileName, outputFileName)
      }
      task.lesson = lesson
      task.name = htmlElement.select("div.header").select("div.title").text()

      task.descriptionText = htmlElement.outerHtml()
        // This replacement is needed for proper MathJax visualization
        .replace("$$$", "$")
        // Replace picture src property if it starts with slash but not codeforces URL
        .replace("src=\"/", "src=\"${CodeforcesNames.CODEFORCES_URL}/")

      task.feedbackLink = FeedbackLink(
        "${(task.course as CodeforcesCourse).contestUrl}/problem/${task.name.substringBefore(".")}?locale=${task.course.languageCode}"
      )

      CodeforcesLanguageProvider.generateTaskFiles(task)?.forEach {
        task.addTaskFile(it)
      }
      task.addSampleTests(htmlElement.selectFirst("div.sample-test"))
      return task
    }

    private fun isStandardIOType(element: Element): Boolean {
      val text = element.ownText()
      return text.contains(STANDARD_INPUT_REGEX)
    }

    private val STANDARD_INPUT_REGEX = "^(standard|стандарт)[^.]*".toRegex()
  }
}