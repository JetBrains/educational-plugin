package com.jetbrains.edu.learning.codeforces.courseFormat

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil.join
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduNames.*
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
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

open class CodeforcesTask : Task() {
  open val inputFileName: String = "input.txt"
  open val outputFileName: String = "output.txt"
  open val latestOutputFileName: String = "latest_output.txt"

  override val itemType: String = CODEFORCES_TASK_TYPE

  fun getTestFolders(project: Project): Array<out VirtualFile> {
    return getDir(project.courseDir)?.findChild(TEST_DATA_FOLDER)?.children.orEmpty()
      .filter { it.isValidCodeforcesTestFolder(this) }.toTypedArray()
  }

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
    val text = innerElement.first().childNodes().joinToString("") { node ->
      when {
        node is TextNode -> node.wholeText
        node is Element && node.tagName() == "br" -> "\n"
        else -> {
          LOG.info("Unexpected element: $node")
          ""
        }
      }
    }.trimEnd()

    val path = join(listOf(TEST_DATA_FOLDER, testFolderName, fileName), VFS_SEPARATOR_CHAR.toString())
    addTaskFile(TaskFile(path, text))
  }

  override fun supportSubmissions(): Boolean = true

  companion object {
    private val LOG: Logger = Logger.getInstance(CodeforcesTask::class.java)

    fun create(htmlElement: Element, lesson: Lesson, index: Int): CodeforcesTask {
      val isStandardIO = htmlElement.select("div.input-file, div.output-file").all { isStandardIOType(it) }

      val task = if (isStandardIO) {
        CodeforcesTask()
      }
      else {
        val inputFileName = htmlElement.selectFirst("div.input-file").ownText()
        val outputFileName = htmlElement.selectFirst("div.output-file").ownText()
        CodeforcesTaskWithFileIO(inputFileName, outputFileName)
      }
      task.parent = lesson
      task.index = index
      // We don't have original problems ids here, so we have to use index to bind them with solutions
      task.id = index
      task.name = htmlElement.select("div.header").select("div.title").text()

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
      task.descriptionText = htmlElement.outerHtml()
        // This replacement is needed for proper MathJax visualization
        .replace("$$$", "$")

      task.feedbackLink = codeforcesTaskLink(task)

      CodeforcesLanguageProvider.generateTaskFiles(task)?.forEach {
        task.addTaskFile(it)
      }

      val sampleTests = htmlElement.selectFirst("div.sample-test")
      @Suppress("SENSELESS_COMPARISON")
      if (sampleTests != null) {
        task.addSampleTests(sampleTests)
      }
      return task
    }

    fun codeforcesSubmitLink(task: Task): String {
      val course = task.course as CodeforcesCourse
      return "${course.getContestUrl()}/${CODEFORCES_SUBMIT}?locale=${course.languageCode}" +
             "&programTypeId=${course.programTypeId ?: codeforcesDefaultProgramTypeId(course)}" +
             "&submittedProblemIndex=${task.presentableName.substringBefore(".")}"
    }

    fun codeforcesTaskLink(task: Task): String {
      val course = task.course as CodeforcesCourse
      return "${course.getContestUrl()}/problem/${task.name.substringBefore(".")}?locale=${course.languageCode}"
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

    // Only for backwards compatibility. Don't use or update it
    private const val GO_TYPE_ID = 32
    private const val JAVA_8_TYPE_ID = 36
    private const val JAVA_11_TYPE_ID = 60
    private const val JAVASCRIPT_TYPE_ID = 34
    private const val KOTLIN_TYPE_ID = 48
    private const val PYTHON_2_TYPE_ID = 7
    private const val PYTHON_3_TYPE_ID = 31
    private const val RUST_TYPE_ID = 75
    private const val SCALA_TYPE_ID = 20

    private const val ESPRESSO_CODEFORCES_COM = "//espresso.codeforces.com/"
    private val TRAILING_SLASH = "^/".toRegex()
    private val URL_WITH_TRAILING_SLASH = "^/.+".toRegex()
    private val STANDARD_INPUT_REGEX = "^(standard|стандарт)[^.]*".toRegex()
  }
}