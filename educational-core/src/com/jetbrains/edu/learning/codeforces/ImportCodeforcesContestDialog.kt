package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.HyperlinkLabel
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class ImportCodeforcesContestDialog(private val showViewAllLabel: Boolean) : DialogWrapper(false) {
  private var contestPanel = ImportCodeforcesContestPanel()
  private val contestsList = HyperlinkLabel(EduCoreBundle.message("link.label.codeforces.view.all.contests"))

  override fun createCenterPanel(): JComponent? = contestPanel.panel

  override fun createSouthPanel(): JComponent {
    val south = super.createSouthPanel()
    if (showViewAllLabel) {
      south.add(contestsList, BorderLayout.WEST)
    }
    return south
  }

  public override fun doValidate(): ValidationInfo? {
    if (!contestPanel.isValidCodeforcesLink()) {
      return ValidationInfo(EduCoreBundle.message("dialog.message.codeforces.contest.url.invalid"))
    }
    return super.doValidate()
  }

  override fun getPreferredFocusedComponent(): JComponent? = contestPanel.preferredFocusedComponent

  init {
    title = EduCoreBundle.message("dialog.title.codeforces.start.contest")
    val scope = CoroutineScope(AppUIExecutor.onUiThread(ModalityState.any()).coroutineDispatchingContext())
    contestsList.addHyperlinkListener { scope.launch { codeforcesListPanel() } }

    init()
  }

  fun getContestId(): Int = contestPanel.getContestId()

  private suspend fun codeforcesListPanel() {
    // TODO: get from extension when it's registered
    val providers = CodeforcesPlatformProviderFactory().getProviders()
    val courses = withContext(Dispatchers.IO) { providers.flatMap { it.loadCourses() }.flatMap { it.courses } }
    if (courses.isNullOrEmpty()) {
      // TODO make it clickable
      // TODO also check other places
      Messages.showErrorDialog(
        EduCoreBundle.message(
          "codeforces.error.failed.to.load.contests.message",
          CodeforcesNames.CODEFORCES_TITLE,
          CodeforcesNames.CODEFORCES_URL
        ),
        EduCoreBundle.message("codeforces.error.failed.to.load.contests.title", CodeforcesNames.CODEFORCES_TITLE)
      )
    }
    else {
      browseContests(courses)
    }
  }

  private fun browseContests(courses: List<Course>) {
    val contestPanel = BrowseContestsPanel(courses)

    val builder = DialogBuilder()
    builder.setTitle(EduCoreBundle.message("dialog.title.codeforces.contests"))
    builder.centerPanel(contestPanel)
    builder.setPreferredFocusComponent(contestPanel.coursesList)
    builder.addActionDescriptor(DialogBuilder.ActionDescriptor {
      object : AbstractAction(EduCoreBundle.message("codeforces.select")) {
        override fun actionPerformed(e: ActionEvent?) {
          it.close(OK_EXIT_CODE)
        }
      }
    })

    contestPanel.coursesList.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent?) {
        val clickCount = e?.clickCount ?: return
        if (clickCount == 2) {
          builder.dialogWrapper.close(OK_EXIT_CODE)
        }
      }
    })

    if (builder.showAndGet()) {
      this.contestPanel.contestURLTextField.text = CodeforcesContestConnector.getContestURLFromID(contestPanel.coursesList.selectedValue.id)
    }
  }
}