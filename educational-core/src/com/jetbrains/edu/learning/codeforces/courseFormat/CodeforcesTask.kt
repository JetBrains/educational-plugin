package com.jetbrains.edu.learning.codeforces.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames.GO
import com.jetbrains.edu.learning.EduNames.JAVA
import com.jetbrains.edu.learning.EduNames.JAVASCRIPT
import com.jetbrains.edu.learning.EduNames.KOTLIN
import com.jetbrains.edu.learning.EduNames.PYTHON
import com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION
import com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION
import com.jetbrains.edu.learning.EduNames.RUST
import com.jetbrains.edu.learning.EduNames.SCALA
import com.jetbrains.edu.learning.codeforces.CodeforcesLanguageProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_SUBMIT
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_TASK_TYPE
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TEST_DATA_FOLDER
import com.jetbrains.edu.learning.codeforces.CodeforcesUtils.isValidCodeforcesTestFolder
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTaskBase
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

open class CodeforcesTask : OutputTaskBase() {

  override val itemType: String = CODEFORCES_TASK_TYPE
  private var _problemIndex: String? = null

  val problemIndex: String get() = _problemIndex ?: presentableName.substringBefore(".")

  override val course: CodeforcesCourse
    get() = super.course as CodeforcesCourse

  fun getTestFolders(project: Project): Array<out VirtualFile> {
    return getDir(project.courseDir)?.findChild(TEST_DATA_FOLDER)?.children.orEmpty()
      .filter { it.isValidCodeforcesTestFolder(this) }.toTypedArray()
  }

  private fun addSampleTests(htmlElement: Element) {
    htmlElement.select("div.input").forEachIndexed { index, inputElement ->
      val testFolderName = (index + 1).toString()
      addTestTaskFile(inputElement, testFolderName, inputFileName)

      val outputElement = inputElement.nextElementSibling() ?: error("HTML element is null")
      addTestTaskFile(outputElement, testFolderName, outputFileName)
    }
  }

  private fun addTestTaskFile(htmlElement: Element, testFolderName: String, fileName: String) {
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

    val path = join(listOf(TEST_DATA_FOLDER, testFolderName, fileName), VFS_SEPARATOR_CHAR.toString())
    addTaskFile(TaskFile(path, text))
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CodeforcesTask::class.java)

    fun create(problemHolder: Element, lesson: Lesson, index: Int): CodeforcesTask {
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
      task._problemIndex = problemHolder.attr("problemindex").takeIf { it.isNotEmpty() }

      htmlElement.select("img").forEach {
        var srcValue = it.attr("src")
        if (srcValue.startsWith(ESPRESSO_CODEFORCES_COM)) {
          srcValue = srcValue.replace(ESPRESSO_CODEFORCES_COM, "https:$ESPRESSO_CODEFORCES_COM")
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

      task.feedbackLink = codeforcesTaskLink(task)

      CodeforcesLanguageProvider.generateTaskFiles(task)?.forEach {
        task.addTaskFile(it)
      }

      val sampleTests = htmlElement.selectFirst("div.sample-test")
      if (sampleTests != null) {
        task.addSampleTests(sampleTests)
      }
      return task
    }

    fun codeforcesSubmitLink(task: CodeforcesTask): String {
      val course = task.course
      return "${course.getContestUrl()}/${CODEFORCES_SUBMIT}?locale=${course.languageCode}" +
             "&programTypeId=${course.programTypeId ?: codeforcesDefaultProgramTypeId(course)}" +
             "&submittedProblemIndex=${task.problemIndex}"
    }

    fun codeforcesTaskLink(task: CodeforcesTask): String {
      val course = task.course
      return "${course.getContestUrl()}/problem/${task.problemIndex}?locale=${course.languageCode}"
    }

    @Deprecated("Only for backwards compatibility. Use CodeforcesCourse.programTypeId")
    fun codeforcesDefaultProgramTypeId(course: CodeforcesCourse): String? {
      val languageID = course.languageID
      val languageVersion = course.languageVersion
      return when {
        GO == languageID -> GO_TYPE_ID
        JAVA == languageID && "8" == languageVersion-> JAVA_8_TYPE_ID
        JAVA == languageID && "11" == languageVersion -> JAVA_11_TYPE_ID
        JAVASCRIPT == languageID -> JAVASCRIPT_TYPE_ID
        KOTLIN == languageID -> KOTLIN_TYPE_ID
        PYTHON == languageID && PYTHON_2_VERSION == languageVersion -> PYTHON_2_TYPE_ID
        PYTHON == languageID && PYTHON_3_VERSION == languageVersion -> PYTHON_3_TYPE_ID
        RUST == languageID -> RUST_TYPE_ID
        SCALA == languageID -> SCALA_TYPE_ID
        //only for tests
        "TEXT" == languageID -> TEXT_TYPE_ID
        else -> {
          LOG.warn("Programming language was not detected: $languageID $languageVersion")
          null
        }
      }?.toString()
    }

    private fun isStandardIOType(element: Element): Boolean {
      val text = element.ownText()
      return text.contains(STANDARD_INPUT_REGEX)
    }

    // For backwards compatibility. Don't use or update it
    private const val GO_TYPE_ID = 32
    private const val JAVA_8_TYPE_ID = 36
    private const val JAVA_11_TYPE_ID = 60
    private const val JAVASCRIPT_TYPE_ID = 34
    private const val KOTLIN_TYPE_ID = 48
    private const val PYTHON_2_TYPE_ID = 7
    private const val PYTHON_3_TYPE_ID = 31
    private const val RUST_TYPE_ID = 75
    private const val SCALA_TYPE_ID = 20
    //only for tests
    private const val TEXT_TYPE_ID = 0

    private const val ESPRESSO_CODEFORCES_COM = "//espresso.codeforces.com/"
    private val TRAILING_SLASH = "^/".toRegex()
    private val URL_WITH_TRAILING_SLASH = "^/.+".toRegex()
    private val STANDARD_INPUT_REGEX = "^(standard|стандарт)[^.]*".toRegex()
  }
}