package com.jetbrains.edu.learning.configuration

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.stepik.StepikAuthorizer
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.StepikSolutionsLoader
import com.jetbrains.edu.learning.stepik.SubmissionsManager.getSubmissionsFromMemory
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.SolutionFile
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.ui.taskDescription.TaskDescriptionView
import com.jetbrains.edu.learning.ui.taskDescription.check.CheckDetailsPanel
import com.jetbrains.edu.learning.ui.taskDescription.createTextPane
import com.jetbrains.edu.learning.ui.taskDescription.styleManagers.StyleManager
import icons.EducationalCoreIcons
import java.awt.BorderLayout
import java.awt.Component.LEFT_ALIGNMENT
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Collectors
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import kotlin.math.roundToInt

abstract class EduConfiguratorWithSubmissions<Settings> : EduConfigurator<Settings> {
  
  override fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    if (currentTask !is EduTask && currentTask !is CodeTask && currentTask !is ChoiceTask) return null
    val course = currentTask.course
    if (course !is EduCourse) return null

    val descriptionText = StringBuilder()
    val textPane: JTextPane = createTextPane()
    val submissionsPanel = createSubmissionsPanel(textPane, project)

    if (EduSettings.isLoggedIn()) {
      val submissions = getSubmissionsFromMemory(currentTask.id) ?: return null
      when {
        submissions.isEmpty() -> descriptionText.append("<a ${StyleManager().textStyleHeader}>You have no submissions yet")
        currentTask is ChoiceTask -> addViewOnStepikLink(descriptionText, currentTask, textPane)
        else -> {
          addSubmissionsToText(submissions, descriptionText)
          textPane.addHyperlinkListener(getSubmissionsListener(currentTask, project))
        }
      }
    }
    else {
      addLoginLink(descriptionText, textPane)
    }

    textPane.text = descriptionText.toString()
    return Pair(submissionsPanel, SUBMISSIONS_TAB_NAME)
  }

  private fun addViewOnStepikLink(descriptionText: StringBuilder,
                                  currentTask: ChoiceTask,
                                  textPane: JTextPane) {
    descriptionText.append(
      "<a ${StyleManager().textStyleHeader};color:${ColorUtil.toHex(hyperlinkColor())} " +
      "href=https://stepik.org/submissions/${currentTask.id}?unit=${currentTask.lesson.unitId}\">View submissions</a>" +
      "<a ${StyleManager().textStyleHeader}> for Quiz tasks on Stepik.org")
    textPane.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
  }

  private fun addLoginLink(descriptionText: StringBuilder, textPane: JTextPane) {
    descriptionText.append("<a ${StyleManager().textStyleHeader};color:${ColorUtil.toHex(hyperlinkColor())}" +
                           " href=>Log in to Stepik.org</a><a ${StyleManager().textStyleHeader}> to view submissions")
    textPane.addHyperlinkListener(HyperlinkListener { e ->
      if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        StepikAuthorizer.doAuthorize { EduUtils.showOAuthDialog() }
        EduCounterUsageCollector.loggedIn(StepikNames.STEPIK, EduCounterUsageCollector.AuthorizationPlace.SUBMISSIONS_TAB)
      }
    })
  }

  private fun createSubmissionsPanel(textPane: JTextPane,
                                     project: Project): JPanel {
    val scrollPane = JBScrollPane(textPane)
    scrollPane.border = JBUI.Borders.empty()
    val backLinkPanel = getBackLinkPanel(project)

    val submissionsPanel = JPanel()
    submissionsPanel.layout = BoxLayout(submissionsPanel, BoxLayout.Y_AXIS)
    submissionsPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    submissionsPanel.border = JBUI.Borders.empty(8, 16, 0, 0)

    submissionsPanel.add(backLinkPanel, LEFT_ALIGNMENT)
    submissionsPanel.add(scrollPane, LEFT_ALIGNMENT)

    return submissionsPanel
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
      if (isTestFile(project, virtualFile) || submissionText == null) {
        null
      }
      else {
        val submissionFileContent = DiffContentFactory.getInstance().create(StepikSolutionsLoader.removeAllTags(submissionText),
                                                                            virtualFile.fileType)
      SimpleDiffRequest("Compare your solution with submission", currentFileContent, submissionFileContent, "Local",
                        "Submission")
      }
    }

    DiffManager.getInstance().showDiff(project, SimpleDiffRequestChain(requests), DiffDialogHints.FRAME)
  }

  private fun submissionLink(submission: Submission): String? {
    val time = submission.time ?: return null
    val pictureSize = (StyleManager().bodyFontSize * 0.75).roundToInt()
    val text = formatDate(time)
    return "<a><img src=${getImageUrl(submission.status)} hspace=6 width=${pictureSize} height=${pictureSize}/></a>" +
           "<a ${StyleManager().textStyleHeader};color:${getLinkColor(submission)} href=${submission.id}> ${text}</a>"
  }

  private fun getImageUrl(status: String?): URL? {
    val icon = when (status) {
      EduNames.CORRECT -> EducationalCoreIcons.TaskSolvedNoFrame
      else -> EducationalCoreIcons.TaskFailedNoFrame
    }
    return (icon as IconLoader.CachedImageIcon).url
  }

  private fun hyperlinkColor() = JBColor(0x6894C6, 0x5C84C9)

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
    val formatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    return formatter.format(calendar.time)
  }

  private fun getBackLinkPanel(project: Project): JPanel {
    val backLink = CheckDetailsPanel.LightColoredActionLink(
      "Back to the task description",
      CheckDetailsPanel.SwitchTaskTabAction(project, 0),
      AllIcons.Actions.Back)
    backLink.border = JBUI.Borders.empty(0, 0, 8, 0)
    val separator = JSeparator()
    val backLinkPanel = JPanel(BorderLayout())
    backLinkPanel.background = TaskDescriptionView.getTaskDescriptionBackgroundColor()
    backLinkPanel.border = JBUI.Borders.empty(0, 0, 8, 15)
    backLinkPanel.add(backLink, BorderLayout.NORTH)
    backLinkPanel.add(separator, BorderLayout.SOUTH)
    backLinkPanel.preferredSize = JBUI.size(Int.MAX_VALUE, 30)
    backLinkPanel.maximumSize = backLinkPanel.preferredSize
    return backLinkPanel
  }

  private fun getSubmissionsListener(task: Task, project: Project): HyperlinkListener {
    return HyperlinkListener { e ->
      if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        val submission = getSubmissionsFromMemory(task.id)?.find { it.id.toString() == e.description } ?: return@HyperlinkListener
        val reply = submission.reply ?: return@HyperlinkListener
          runInEdt {
            showDiff(project, task, reply)
          }
      }
    }
  }

  companion object {
    const val SUBMISSIONS_TAB_NAME = "Submissions"
  }
}