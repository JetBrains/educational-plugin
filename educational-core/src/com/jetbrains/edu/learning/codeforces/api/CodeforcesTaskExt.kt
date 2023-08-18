package com.jetbrains.edu.learning.codeforces.api

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTaskWithFileIO
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

private const val ESPRESSO_CODEFORCES_COM = "//espresso.codeforces.com/"
private const val ERROR = "Parsing failed. Unable to find CSS elements:"
private val TRAILING_SLASH = "^/".toRegex()
private val URL_WITH_TRAILING_SLASH = "^/.+".toRegex()
private val STANDARD_INPUT_REGEX = "^(standard|стандарт)[^.]*".toRegex()

fun CodeforcesCourse.parseResponseToAddContent(doc: Document) {

  name = doc.selectFirst(".caption")?.text() ?: error("$ERROR caption")
  val problems = doc.select(".problemindexholder") ?: error("$ERROR problemindexholder")

  description = problems.joinToString("\n") {
    it.select("div.header").select("div.title").text() ?: error("$ERROR div.header, div.title")
  }

  val lesson = Lesson()
  lesson.name = CodeforcesNames.CODEFORCES_PROBLEMS
  lesson.parent = this

  addLesson(lesson)
  problems.forEachIndexed { index, task -> lesson.addTask(createCodeforcesTask(task, lesson, index + 1)) }
}

fun createCodeforcesTask(problemHolder: Element, lesson: Lesson, index: Int): CodeforcesTask {
  val htmlElement = problemHolder.selectFirst(".problem-statement") ?: error("")

  val isStandardIO = htmlElement.select("div.input-file, div.output-file").all { isStandardIOType(it) }

  val task = if (isStandardIO) {
    CodeforcesTask()
  }
  else {
    val inputFileName = htmlElement.selectFirst("div.input-file")?.ownText() ?: error("No input file found")
    val outputFileName = htmlElement.selectFirst("div.output-file")?.ownText() ?: error("No output file found")
    CodeforcesTaskWithFileIO(inputFileName, outputFileName)
  }
  task.parent = lesson
  task.index = index
  // We don't have original problems ids here, so we have to use index to bind them with solutions
  task.id = index
  task.name = htmlElement.select("div.header").select("div.title").text()
  task.problemIndex = problemHolder.attr("problemindex").takeIf { it.isNotEmpty() } ?: task.presentableName.substringBefore(".")

  htmlElement.select("img").forEach {
    var srcValue = it.attr("src")
    if (srcValue.startsWith(ESPRESSO_CODEFORCES_COM)) {
      srcValue = srcValue.replace(ESPRESSO_CODEFORCES_COM, "https:${ESPRESSO_CODEFORCES_COM}")
    }
    else if (srcValue.matches(URL_WITH_TRAILING_SLASH)) {
      srcValue = srcValue.replace(TRAILING_SLASH, "${CodeforcesNames.CODEFORCES_URL}/")
    }
    it.attr("src", srcValue)
  }

  task.descriptionFormat = DescriptionFormat.HTML
  htmlElement.getElementsByClass("test-example-line").append("\n").unwrap()
  task.descriptionText = htmlElement.outerHtml()
    // This replacement is needed for proper MathJax visualization
    .replace("$$$", "$")

  task.feedbackLink = CodeforcesTask.codeforcesTaskLink(task)

  CodeforcesLanguageProvider.generateTaskFiles(task)?.forEach {
    task.addTaskFile(it)
  }

  val sampleTests = htmlElement.selectFirst("div.sample-test")
  if (sampleTests != null) {
    task.addSampleTests(sampleTests)
  }
  return task
}

private fun CodeforcesTask.addSampleTests(htmlElement: Element) {
  htmlElement.select("div.input").forEachIndexed { index, inputElement ->
    val testFolderName = (index + 1).toString()
    addTestTaskFile(inputElement, testFolderName, inputFileName)

    val outputElement = inputElement.nextElementSibling() ?: error("HTML element is null")
    addTestTaskFile(outputElement, testFolderName, outputFileName)
  }
}

private fun CodeforcesTask.addTestTaskFile(htmlElement: Element, testFolderName: String, fileName: String) {
  val innerElement = htmlElement.select("pre")
  if (innerElement.isEmpty()) {
    error("Can't find HTML element with test data in ${htmlElement.text()}")
  }
  val firstInnerElement = innerElement.first() ?:  error("Can't find HTML element with test data")
  val text = firstInnerElement.childNodes().joinToString("") { node ->
    when {
      node is TextNode -> node.wholeText
      node is Element && node.tagName() == "br" -> "\n"
      node is Element && node.tagName() == "div" -> "${node.wholeText()}\n"
      else -> {
        LOG.info("Unexpected element: $node")
        ""
      }
    }
  }.trimEnd()

  val path = listOf(CodeforcesNames.TEST_DATA_FOLDER, testFolderName, fileName).joinToString("/")
  addTaskFile(TaskFile(path, text))
}

private fun isStandardIOType(element: Element): Boolean {
  val text = element.ownText()
  return text.contains(STANDARD_INPUT_REGEX)
}

private val LOG: Logger = Logger.getInstance(CodeforcesTask::class.java)