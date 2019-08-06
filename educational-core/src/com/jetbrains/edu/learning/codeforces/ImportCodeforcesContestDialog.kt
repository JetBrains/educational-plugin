package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.HyperlinkLabel
import com.jetbrains.edu.learning.CoursesProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseLoading.CourseLoader
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JComponent

class ImportCodeforcesContestDialog : DialogWrapper(false) {
  private var contestPanel = ImportCodeforcesContestPanel()
  private val contestsList = HyperlinkLabel("View all contests...")

  override fun createCenterPanel(): JComponent? = contestPanel.panel

  override fun createSouthPanel(): JComponent {
    val south = super.createSouthPanel()
    south.add(contestsList, BorderLayout.WEST)
    return south
  }

  public override fun doValidate(): ValidationInfo? {
    if (!contestPanel.isValidCodeforcesLink()) {
      return ValidationInfo("Contest URL is invalid")
    }
    return super.doValidate()
  }

  override fun getPreferredFocusedComponent(): JComponent? = contestPanel.preferredFocusedComponent

  init {
    title = "Start Codeforces Contest"
    contestsList.addHyperlinkListener { codeforcesListPanel() }

    init()
  }

  fun getContestIdAndLanguage(): Pair<Int, String> = Pair(contestPanel.contestId(), contestPanel.contestLanguage())

  private fun codeforcesListPanel() {
    val courses = CourseLoader.getCourseInfosUnderProgress("Getting Available Contests") {
      CoursesProvider.loadAllCourses(listOf(CodeforcesContestsProvider))
    } ?: return

    if (courses.isEmpty()) {
      // TODO make it clickable
      // TODO also check other places
      Messages.showErrorDialog("Cannot find contests on Codeforces, please check if site is working: ${CodeforcesNames.CODEFORCES_URL}",
                               "Failed to Load Codeforces Contest")
      return
    }

    browseContests(courses)
  }

  private fun browseContests(courses: List<Course>) {
    val contestPanel = BrowseContestsPanel(courses)

    val builder = DialogBuilder()
    builder.setTitle("Contests")
    builder.centerPanel(contestPanel)
    builder.setPreferredFocusComponent(contestPanel.coursesList)
    builder.addActionDescriptor(DialogBuilder.ActionDescriptor {
      object : AbstractAction("Select") {
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