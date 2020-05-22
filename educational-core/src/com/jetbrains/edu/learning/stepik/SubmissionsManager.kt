package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.AdditionalTabPanel
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionToolWindowFactory
import com.jetbrains.edu.learning.taskDescription.ui.styleManagers.StyleManager
import icons.EducationalCoreIcons
import java.net.URL
import java.text.DateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.Collectors
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import kotlin.math.roundToInt

abstract class SubmissionsManager {

  fun putToSubmissions(taskId: Int, submissionsToAdd: MutableList<Submission>) {
    submissions[taskId] = submissionsToAdd
  }

  fun addToSubmissionsMap(taskId: Int, submission: Submission?) {
    if (submission == null) return
    val submissionsList = submissions.getOrPut(taskId) { mutableListOf(submission) }
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      //potential race when loading submissions and checking task at one time
    }
  }

  private fun getSubmissionsFromMemory(taskId: Int): List<Submission>? {
    return submissions[taskId]
  }

  fun createSubmissionsTab(currentTask: Task, course: Course, project: Project): Pair<JPanel, String>? {
    if (!submissionsCanBeShown(course)) return null

    val descriptionText = StringBuilder()
    val submissionsPanel = AdditionalTabPanel(project)
    val submissionsList = getSubmissionsFromMemory(currentTask.id)

    if (isLoggedIn() || submissionsList != null) {
      if (submissionsList == null) return null
      when {
        currentTask is ChoiceTask -> addViewOnPlatformLink(descriptionText, currentTask, submissionsPanel)
        submissionsList.isEmpty() -> descriptionText.append("<a ${StyleManager().textStyleHeader}>${EduCoreBundle.message("submissions.empty")}")
        else -> {
          addSubmissionsToText(submissionsList, descriptionText)
          submissionsPanel.addHyperlinkListener(getSubmissionsListener(currentTask, project, this))
        }
      }
    }
    else {
      addLoginLink(descriptionText, submissionsPanel)
    }

    submissionsPanel.setText(descriptionText.toString())
    return Pair(submissionsPanel, SUBMISSIONS_TAB_NAME)
  }

  fun prepareSubmissionsContent(project: Project, course: Course) {
    val window = ToolWindowManager.getInstance(project).getToolWindow(TaskDescriptionToolWindowFactory.STUDY_TOOL_WINDOW)
    if (window != null) {
      val submissionsContent = window.contentManager.findContent(SUBMISSIONS_TAB_NAME)
      if (submissionsContent != null) {
        val submissionsPanel = submissionsContent.component
        if (submissionsPanel is AdditionalTabPanel) {
          ApplicationManager.getApplication().invokeLater { submissionsPanel.addLoadingPanel(getPlatformName()) }
        }
      }
    }
    loadAllSubmissions(project, course)
  }

  private fun getLastSubmission(taskId: Int, isSolved: Boolean): Submission? {
    val submissions = getSubmissions(taskId, isSolved)
    return submissions.firstOrNull()
  }

  fun getSubmissions(taskId: Int, isSolved: Boolean): List<Submission> {
    val status = if (isSolved) EduNames.CORRECT else EduNames.WRONG
    return getAllSubmissions(taskId).filter { it.status == status }
  }

  fun isLastSubmissionUpToDate(task: Task, isSolved: Boolean): Boolean {
    if (task is TheoryTask) return true
    val submission = getLastSubmission(task.id, isSolved) ?: return false
    return submission.time?.after(task.updateDate) ?: false
  }

  fun getLastSubmissionReply(taskId: Int, isSolved: Boolean): Reply? {
    return getLastSubmission(taskId, isSolved)?.reply
  }

  protected open fun addViewOnPlatformLink(descriptionText: StringBuilder, currentTask: ChoiceTask, submissionsPanel: AdditionalTabPanel) {}

  abstract fun getAllSubmissions(stepIds: Set<Int>): List<Submission>?

  abstract fun getAllSubmissions(stepId: Int): MutableList<Submission>

  protected abstract fun loadAllSubmissions(project: Project, course: Course?)

  protected abstract fun submissionsCanBeShown(course: Course?): Boolean

  protected abstract fun getPlatformName(): String

  protected abstract fun isLoggedIn(): Boolean

  protected abstract fun doAuthorize()

  private fun addLoginLink(descriptionText: StringBuilder,
                           submissionsPanel: AdditionalTabPanel) {
    descriptionText.append("<a ${StyleManager().textStyleHeader};color:${ColorUtil.toHex(hyperlinkColor())}" +
                           " href=>${EduCoreBundle.message("submissions.login", getPlatformName())}" +
                           "</a><a ${StyleManager().textStyleHeader}>")
    submissionsPanel.addHyperlinkListener(HyperlinkListener { e ->
      if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        doAuthorize()
      }
    })
  }

  private fun getSubmissionsListener(task: Task, project: Project, submissionsManager: SubmissionsManager): HyperlinkListener {
    return HyperlinkListener { e ->
      if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        val submission = submissionsManager.getSubmissionsFromMemory(task.id)?.find { it.id.toString() == e.description }
                         ?: return@HyperlinkListener
        val reply = submission.reply ?: return@HyperlinkListener
        runInEdt {
          showDiff(project, task, reply)
        }
      }
    }
  }

  protected fun hyperlinkColor() = JBColor(0x6894C6, 0x5C84C9)

  private fun getImageUrl(status: String?): URL? {
    val icon = when (status) {
      EduNames.CORRECT -> EducationalCoreIcons.TaskSolvedNoFrame
      else -> EducationalCoreIcons.TaskFailedNoFrame
    }
    return (icon as IconLoader.CachedImageIcon).url
  }

  private fun getLinkColor(submission: Submission): String = when (submission.status) {
    EduNames.CORRECT -> "#${ColorUtil.toHex(JBColor(0x368746, 0x499C54))}"
    else -> "#${ColorUtil.toHex(JBColor(0xC7222D, 0xFF5261))}"
  }

  private fun getSubmissionTexts(reply: Reply, taskName: String): Map<String, String>? {
    val solutions = reply.solution
    if (solutions == null) {
      val submissionText = reply.code ?: return null
      return mapOf(taskName to submissionText)
    }
    return solutions.stream().collect(Collectors.toMap(SolutionFile::name, SolutionFile::text))
  }

  private fun formatDate(time: Date): String {
    val calendar = GregorianCalendar()
    calendar.time = time
    val formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.getDefault())
    return formatter.format(calendar.time)
  }

  private fun addSubmissionsToText(submissionsNext: List<Submission>,
                           descriptionText: StringBuilder) {
    for (submission in submissionsNext) {
      descriptionText.append(submissionLink(submission)).append("<br>")
    }
  }

  private fun showDiff(project: Project, task: Task, reply: Reply) {
    val taskFiles = task.taskFiles.values.toMutableList()
    val submissionTexts = getSubmissionTexts(reply, task.name) ?: return
    val requests = taskFiles.mapNotNull {
      val virtualFile = it.getVirtualFile(project) ?: error("VirtualFile for ${it.name} not found")
      val currentFileContent = DiffContentFactory.getInstance().create(VfsUtil.loadText(virtualFile), virtualFile.fileType)
      val submissionText = submissionTexts[it.name] ?: submissionTexts[task.name]
      if (EduUtils.isTestsFile(project, virtualFile) || submissionText == null) {
        null
      }
      else {
        val submissionFileContent = DiffContentFactory.getInstance().create(StepikSolutionsLoader.removeAllTags(submissionText),
                                                                            virtualFile.fileType)
        SimpleDiffRequest(EduCoreBundle.message("submissions.compare"),
                          currentFileContent,
                          submissionFileContent,
                          EduCoreBundle.message("submissions.local"),
                          EduCoreBundle.message("submissions.submission"))
      }
    }
    DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
  }

  private fun submissionLink(submission: Submission): String? {
    val time = submission.time ?: return null
    val pictureSize = (StyleManager().bodyFontSize * 0.75).roundToInt()
    val text = formatDate(time)
    return "<h><img src=${getImageUrl(submission.status)} hspace=6 width=${pictureSize} height=${pictureSize}/></h>" +
           "<a ${StyleManager().textStyleHeader};color:${getLinkColor(submission)} href=${submission.id}> ${text}</a>"
  }

  companion object{
    val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()
    const val SUBMISSIONS_TAB_NAME = "Submissions"
    private val EP_NAME = ExtensionPointName.create<SubmissionsManager>("Educational.submissionsManager")

    fun getSubmissionsManagerForCourse(course: Course?): SubmissionsManager? {
      if(course == null) return null
      val submissionsManagers = EP_NAME.extensionList.filter { it.submissionsCanBeShown(course) }
      if (submissionsManagers.isEmpty()) {
        return null
      }
      if (submissionsManagers.size > 1) {
        error("Several submissionsManagers available for ${course.name}: $submissionsManagers")
      }
      return submissionsManagers[0]
    }

    @VisibleForTesting
    fun clear() {
      submissions.clear()
    }
  }
}