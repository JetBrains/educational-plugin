package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.DEFAULT_TERMS_OF_AGREEMENT
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.TERMS_OF_AGREEMENT
import com.jetbrains.edu.learning.codeforces.CodeforcesPlatformProvider
import com.jetbrains.edu.learning.codeforces.CodeforcesSettings
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.api.RegistrationCompleted
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.*
import com.jetbrains.edu.learning.newproject.ui.coursePanel.*
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseDetailsPanel.Companion.formatNumber
import com.jetbrains.edu.learning.ui.EduHyperlinkLabel
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Font
import java.net.URL
import java.time.format.DateTimeFormatter
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingConstants

private const val EM_DASH = "\u2014"

class CodeforcesCoursePanel(disposable: Disposable) : CoursePanel(disposable, false) {

  init {
    buttonsPanel.border = JBUI.Borders.empty()
  }

  override fun addComponents() {
    with(content) {
      add(tagsPanel)
      add(titlePanel)
      add(authorsPanel)
      add(errorComponent)
      add(courseDetailsPanel)
      add(RegistrationPanel())
      add(settingsPanel)
    }
  }

  override fun startButtonText(course: Course?): String {
    if (course !is CodeforcesCourse) {
      return super.startButtonText(course)
    }

    return when {
      !course.isUpcomingContest -> EduCoreBundle.message("course.dialog.start.button.codeforces.practice")
      course.isOngoing -> EduCoreBundle.message("course.dialog.start.button.codeforces.start.contest")
      else -> EduCoreBundle.message("course.dialog.start.button.codeforces.register")
    }
  }

  override val openButtonText: String
    get() = EduCoreBundle.message("course.dialog.start.button.codeforces.open.contest")

  override fun validateSettings(it: Course): ValidationMessage? = null

  override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
    val codeforcesCourse = info.course as? CodeforcesCourse ?: return
    if (codeforcesCourse.isRegistrationOpen && !codeforcesCourse.isOngoing) {
      val registrationLink = CodeforcesNames.CODEFORCES_URL + codeforcesCourse.registrationLink
      register(codeforcesCourse.id, registrationLink)
    }
    else {
      CodeforcesPlatformProvider().joinAction(info, mode, this)
    }
  }

  override fun createCourseDetailsPanel(): NonOpaquePanel {
    return ContestDetailsPanel()
  }

  private inner class RegistrationPanel : NonOpaquePanel(), CourseSelectionListener {
    private val hyperlinkLabel: LinkLabel<CodeforcesCourse> = LinkLabel<CodeforcesCourse>(
      EduCoreBundle.message("codeforces.registration.for.submit"), AllIcons.Ide.External_link_arrow).apply {
      horizontalTextPosition = SwingConstants.LEFT
      border = JBUI.Borders.emptyLeft(10)
    }

    init {
      add(buttonsPanel, BorderLayout.LINE_START)
      add(hyperlinkLabel, BorderLayout.CENTER)
    }

    override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
      val codeforcesCourse = courseInfo.course as? CodeforcesCourse ?: return
      hyperlinkLabel.apply {
        isVisible = codeforcesCourse.isRegistrationOpen == true && codeforcesCourse.isOngoing
        setListener(LinkListener { _, course ->
          val registrationLink = CodeforcesNames.CODEFORCES_URL + course.registrationLink
          register(codeforcesCourse.id, registrationLink)
        }, codeforcesCourse)
      }

      buttonsPanel.onCourseSelectionChanged(courseInfo, courseDisplaySettings)
    }
  }

  private fun register(contestId: Int, registrationLink: String) {
    if (CodeforcesSettings.getInstance().isLoggedIn()) {
      registerFromIDE(contestId, registrationLink)
    }
    else {
      BrowserUtil.browse(URL(registrationLink))
    }
  }

  private fun registerFromIDE(id: Int, registrationLink: String) {
    val connector = CodeforcesConnector.getInstance()
    val registrationData = connector.getRegistrationData(id)
    if (registrationData is RegistrationCompleted) {
      if (TermsOfAgreementDialog(registrationData.termsOfAgreement, registrationLink,
                                 registrationData.isTeamRegistrationAvailable).showAndGet()) {
        if (connector.registerToContest(id, registrationData.token)) {
          Messages.showInfoMessage(EduCoreBundle.message("codeforces.registration.completed"),
                                   EduCoreBundle.message("codeforces.contest.registration"))
          return
        }
      }
      else {
        return
      }
    }
    Messages.showErrorDialog(EduCoreBundle.message("codeforces.registration.failed", registrationLink),
                             EduCoreBundle.message("codeforces.contest.registration.failed"))
  }
}

private class ContestDetailsPanel : NonOpaquePanel(), CourseSelectionListener {
  private val startsTextLabel: JBLabel = createBoldLabel(EduCoreBundle.message("codeforces.course.selections.starts"))
  private val startsLabel: JBLabel = JBLabel()

  private val durationTextLabel: JBLabel = createBoldLabel(EduCoreBundle.message("codeforces.course.selection.duration"))
  private val durationLabel: JBLabel = JBLabel()

  private val timeRemainingTextLabel: JBLabel = createBoldLabel(EduCoreBundle.message("codeforces.course.selections.remaining"))
  private val timeRemainingLabel: JBLabel = JBLabel()

  private val finishedTextLabel: JBLabel = createBoldLabel(EduCoreBundle.message("codeforces.course.selection.finished"))
  private val finishedLabel: JBLabel = JBLabel()

  private val participantsTextLabel: JBLabel = createBoldLabel(EduCoreBundle.message("codeforces.course.selection.participants"))
  private val participantsLabel: JBLabel = JBLabel()

  private val grayTextInfoPanel: GrayTextHtmlPanel = GrayTextHtmlPanel("").apply {
    border = JBUI.Borders.empty(10, 0, 0, 0)
  }

  private val informationComponent: ErrorComponent = ErrorComponent(null,
                                                                    doValidation = { course -> doValidation(course) },
                                                                    errorPanelTopBottomMargin = 5,
                                                                    errorPanelLeftMargin = 10,
                                                                    icon = AllIcons.General.BalloonInformation).apply {
    border = JBUI.Borders.emptyTop(10)
  }

  init {
    border = JBUI.Borders.empty(8, HORIZONTAL_MARGIN, 5, 0)

    val headersPanel = NonOpaquePanel().apply {
      layout = VerticalFlowLayout(0, 6)
      border = JBUI.Borders.emptyRight(18)
      add(startsTextLabel)
      add(durationTextLabel)
      add(finishedTextLabel)
      add(timeRemainingTextLabel)
      add(participantsTextLabel)
    }

    val valuePanel = NonOpaquePanel().apply {
      layout = VerticalFlowLayout(0, 6)
      add(startsLabel)
      add(durationLabel)
      add(finishedLabel)
      add(timeRemainingLabel)
      add(participantsLabel)
    }

    add(headersPanel, BorderLayout.LINE_START)
    add(valuePanel, BorderLayout.CENTER)
    val panel = NonOpaquePanel().apply {
      add(informationComponent, BorderLayout.NORTH)
      add(grayTextInfoPanel, BorderLayout.SOUTH)
    }
    add(panel, BorderLayout.PAGE_END)
  }

  private fun doValidation(course: Course?) {
    informationComponent.isVisible = false
    if (course !is CodeforcesCourse) return

    if (!(course.isUpcomingContest && !course.isOngoing)) {
      return
    }

    if (course.isRegistrationOpen) {
      val userRegisteredForContest = CodeforcesConnector.getInstance().isUserRegisteredForContest(course.id)
      if (userRegisteredForContest) {
        grayTextInfoPanel.text = ""
        val validationMessage = ValidationMessage(
          EduCoreBundle.message("codeforces.you.are.registered.for.the.contest"),
          type = ValidationMessageType.INFO)
        informationComponent.setErrorMessage(validationMessage)
        informationComponent.isVisible = true
      }
      return
    }

    val registrationCountdown = course.registrationCountdown ?: error("registration countdown is null for '${course.id}'")
    val registrationOpensIn = humanReadableDuration(registrationCountdown)
    val validationMessage = ValidationMessage(
      EduCoreBundle.message("codeforces.course.selection.registration.opens.in", registrationOpensIn),
      type = ValidationMessageType.INFO)
    informationComponent.setErrorMessage(validationMessage)
    informationComponent.isVisible = true

  }

  private fun createBoldLabel(text: String) = JBLabel(text).apply {
    font = font.deriveFont(Font.BOLD, 13.0f)
  }

  override fun onCourseSelectionChanged(courseInfo: CourseInfo, courseDisplaySettings: CourseDisplaySettings) {
    val codeforcesCourse = courseInfo.course as CodeforcesCourse

    val dateTimeFormatter = DateTimeFormatter.ofPattern(CourseDetailsPanel.DATE_TIME_PATTERN)
    startsLabel.text = codeforcesCourse.startDate?.format(dateTimeFormatter)
    durationLabel.text = humanReadableDuration(codeforcesCourse.length)
    finishedLabel.text = codeforcesCourse.endDateTime?.format(dateTimeFormatter)

    val remainingTime = codeforcesCourse.remainingTime
    if (remainingTime != null) {
      timeRemainingLabel.text = if (remainingTime.toHoursPart() > 0) {
        EduCoreBundle.message("codeforces.course.selection.duration.value.hours", remainingTime.toHoursPart(),
                              remainingTime.toMinutesPart())
      }
      else {
        EduCoreBundle.message("codeforces.course.selection.duration.value.min", remainingTime.toMinutesPart())
      }
    }
    participantsLabel.text = if (codeforcesCourse.participantsNumber > 0) formatNumber(codeforcesCourse.participantsNumber) else EM_DASH

    grayTextInfoPanel.setBody(when {
                                !codeforcesCourse.isUpcomingContest && !codeforcesCourse.isOngoing -> {
                                  EduCoreBundle.message("codeforces.past.contest.description")
                                }
                                codeforcesCourse.isUpcomingContest && codeforcesCourse.isRegistrationOpen -> {
                                  EduCoreBundle.message("codeforces.registration.description")
                                }
                                else -> ""
                              })

    informationComponent.onCourseSelectionChanged(courseInfo, courseDisplaySettings)

    updateVisibility(codeforcesCourse)
  }

  private fun updateVisibility(codeforcesCourse: CodeforcesCourse) {
    val isUpcomingContest = codeforcesCourse.isUpcomingContest
    val isRunningContest = codeforcesCourse.isOngoing

    startsTextLabel.isVisible = isUpcomingContest
    startsLabel.isVisible = isUpcomingContest

    durationTextLabel.isVisible = isUpcomingContest
    durationLabel.isVisible = isUpcomingContest

    finishedTextLabel.isVisible = !isUpcomingContest && !isRunningContest
    finishedLabel.isVisible = !isUpcomingContest && !isRunningContest

    timeRemainingTextLabel.isVisible = isRunningContest
    timeRemainingLabel.isVisible = isRunningContest

    participantsTextLabel.isVisible = true
    participantsLabel.isVisible = true

    grayTextInfoPanel.isVisible = grayTextInfoPanel.text.isNotEmpty()
  }
}

private class TermsOfAgreementDialog(private val termsOfAgreement: String,
                                     private val registrationLink: String,
                                     private val isTeamRegistrationAvailable: Boolean) : DialogWrapper(false) {
  init {
    title = EduCoreBundle.message("codeforces.contest.registration")
    setOKButtonText(EduCoreBundle.message("codeforces.register.as.individual"))
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val text = if (termsOfAgreement == DEFAULT_TERMS_OF_AGREEMENT) TERMS_OF_AGREEMENT else termsOfAgreement.replace("\n", "<br>")
    return panel {
      row() {
        EduHyperlinkLabel(text)()
      }
    }.withBorder(JBUI.Borders.empty(0, 10))
  }

  override fun createNorthPanel(): JComponent? {
    if (!isTeamRegistrationAvailable) return super.createNorthPanel()
    val jPanel = JPanel(BorderLayout())
    jPanel.border = JBUI.Borders.empty(6, 16)
    val jbLabel = EduHyperlinkLabel(EduCoreBundle.message("codeforces.team.registration.notice", registrationLink))
    jbLabel.alignmentX = Component.CENTER_ALIGNMENT
    jbLabel.border = JBUI.Borders.empty(0, 0, 12, 16)
    val jSeparator = JSeparator()
    jPanel.add(jbLabel, BorderLayout.CENTER)
    jPanel.add(jSeparator, BorderLayout.SOUTH)
    return jPanel
  }

}