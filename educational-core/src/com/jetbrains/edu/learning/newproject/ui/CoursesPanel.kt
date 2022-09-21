package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.ui.JBCardLayout
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorState
import com.jetbrains.edu.learning.newproject.ui.filters.CoursesSearchComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.JPanel


private const val CONTENT_CARD_NAME = "CONTENT"
private const val LOADING_CARD_NAME = "PROGRESS"
private const val NO_COURSES = "NO_COURSES"

abstract class CoursesPanel(
  private val coursesProvider: CoursesPlatformProvider,
  private val scope: CoroutineScope,
  disposable: Disposable
) : JPanel() {

  @VisibleForTesting
  @Suppress("LeakingThis")
  var coursePanel: CoursePanel = createCoursePanel(disposable)

  @Suppress("LeakingThis")
  protected val coursesSearchComponent: CoursesSearchComponent = CoursesSearchComponent(getEmptySearchText(),
                                                                                        { coursesGroups },
                                                                                        { groups -> updateModel(groups, selectedCourse) })

  private val coursesListDecorator = CoursesListDecorator(this.createCoursesListPanel(), this.tabDescription(), this.toolbarAction())
  private val loginPanel: LoginPanel? by lazy { getLoginComponent() }

  private val cardLayout = JBCardLayout()
  protected val coursesGroups = mutableListOf<CoursesGroup>()
  protected val noCoursesPanel: JBPanelWithEmptyText = JBPanelWithEmptyText()

  @Volatile
  private var loadingFinished = false

  val languageSettings get() = coursePanel.languageSettings
  val selectedCourse get() = coursesListDecorator.getSelectedCourse()
  val locationString: String?
    get() {
      // We use `coursePanel` with location field
      // so `coursePanel.locationString` must return not null value
      return coursePanel.locationString
    }

  init {
    layout = cardLayout
    background = MAIN_BG_COLOR
    coursesListDecorator.setSelectionListener { this.processSelectionChanged() }
    this.setNoCoursesPanelDefaultText()

    this.add(createContentPanel(), CONTENT_CARD_NAME)
    this.add(createLoadingPanel(), LOADING_CARD_NAME)
    this.add(noCoursesPanel, NO_COURSES)
    showProgressState()
  }

  open fun updateModelAfterCourseDeletedFromStorage(deletedCourse: Course) {
    updateModel(coursesGroups, null, true)
  }

  open fun getEmptySearchText(): String = EduCoreBundle.message("course.dialog.search.placeholder")

  fun hideLoginPanel() {
    loginPanel?.isVisible = false
  }

  fun showLoginPanel() {
    loginPanel?.isVisible = true
  }

  open fun showErrorMessage(e: CoursesDownloadingException) {}

  private fun createContentPanel(): JPanel {
    val searchAndLoginPanel = NonOpaquePanel()
    searchAndLoginPanel.add(coursesSearchComponent, BorderLayout.NORTH)
    loginPanel?.apply {
      searchAndLoginPanel.add(this, BorderLayout.SOUTH)
    }

    val mainPanel = JPanel(BorderLayout()).apply {
      add(searchAndLoginPanel, BorderLayout.PAGE_START)
      add(createSplitPane(), BorderLayout.CENTER)
      background = MAIN_BG_COLOR
    }

    return mainPanel
  }

  suspend fun loadCourses() {
    try {
      coursesGroups.addAll(withContext(Dispatchers.IO) {
        coursesProvider.loadCourses()
      })
    }
    catch (e: CoursesDownloadingException) {
      showErrorMessage(e)
    }

    loadingFinished = true
    if (isShowing) {
      onTabSelection()
    }
  }

  fun onTabSelection() {
    if (loadingFinished) {
      updateFilters(coursesGroups)
      updateModel(coursesGroups, null)
      showContent(coursesGroups.isEmpty())
      if (!isLoginNeeded()) {
        hideLoginPanel()
      }
    }
  }

  private fun createSplitPane(): JPanel {
    val splitPane = OnePixelSplitter()
    splitPane.firstComponent = coursesListDecorator
    splitPane.secondComponent = coursePanel
    splitPane.divider.background = CoursePanel.DIVIDER_COLOR
    splitPane.proportion = 0.46f

    val splitPaneRoot = NonOpaquePanel() // needed to set borders
    splitPaneRoot.add(splitPane, BorderLayout.CENTER)
    splitPaneRoot.border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 1, 0, 0, 0)
    return splitPaneRoot
  }

  protected open fun toolbarAction(): ToolbarActionWrapper? = null

  protected open fun tabDescription(): String? = null

  protected open fun getLoginComponent(): LoginPanel? = null

  private fun createLoadingPanel() = JPanel(BorderLayout()).apply {
    add(CenteredIcon(), BorderLayout.CENTER)
  }

  protected open fun setNoCoursesPanelDefaultText() {
    val text = noCoursesPanel.emptyText
    text.text = EduCoreBundle.message("course.dialog.no.courses", ApplicationNamesInfo.getInstance().fullProductName)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide1") + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    @Suppress("DialogTitleCapitalization") // it's ok to start from lowercase as it's the second part of a sentence
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide2"),
                             SimpleTextAttributes.LINK_ATTRIBUTES,
                             ActionListener { EduBrowser.getInstance().browse(EduNames.NO_COURSES_URL) })
  }

  private fun showProgressState() = cardLayout.show(this, LOADING_CARD_NAME)

  private fun showContent(empty: Boolean) {
    if (empty) {
      cardLayout.show(this, NO_COURSES)
      return
    }
    cardLayout.show(this, CONTENT_CARD_NAME)
  }

  protected open fun updateFilters(coursesGroups: List<CoursesGroup>) {
    coursesSearchComponent.updateFilters(coursesGroups)
  }

  protected open fun createCoursesListPanel(): CoursesListPanel = CoursesListWithResetFilters()

  open fun processSelectionChanged() {
    val course = selectedCourse
    if (course != null) {
      coursePanel.bindCourse(course)
    }
    else {
      coursePanel.showEmptyState()
    }
    doValidation()
  }

  fun doValidation() {
    coursePanel.doValidation()
  }

  fun setError(errorState: ErrorState) {
    coursePanel.setError(errorState)
    revalidate()
  }

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?, filterCourses: Boolean = true) {
    if (filterCourses) {
      val filteredCoursesGroups = coursesGroups.map {
        CoursesGroup(it.name, coursesSearchComponent.filterCourses(it.courses))
      }
      coursesListDecorator.updateModel(filteredCoursesGroups, courseToSelect)
    }
    else {
      coursesListDecorator.updateModel(coursesGroups, courseToSelect)
    }
  }

  fun setButtonsEnabled(canStartCourse: Boolean) {
    coursePanel.setButtonsEnabled(canStartCourse)
  }

  fun scheduleUpdateAfterLogin() {
    scope.launch {
      updateCoursesAfterLogin()
    }
  }

  protected open suspend fun updateCoursesAfterLogin(preserveSelection: Boolean = true) {
    updateFilters(coursesGroups)
    showContent(coursesGroups.isEmpty())

    // hack: selection in com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel.updateModel can't scroll correctly
    // as all the child components have 0 bounds at the moment of update
    val courseToSelect = selectedCourse
    updateModel(coursesGroups, null)
    if (preserveSelection) {
      coursesListDecorator.setSelectedValue(courseToSelect)
    }
  }

  protected open fun isLoginNeeded() = false

  open inner class CoursesListWithResetFilters : CoursesListPanel() {

    override fun resetFilters() {
      coursesSearchComponent.resetSearchField()
      coursesSearchComponent.resetSelection()
      updateModel(coursesGroups, null, true)
    }
  }

  protected open fun createCoursePanel(disposable: Disposable): CoursePanel {
    return DialogCoursePanel(disposable)
  }

  private inner class DialogCoursePanel(disposable: Disposable) : CoursePanel(disposable, true) {
    override fun joinCourseAction(info: CourseInfo, mode: CourseMode) {
      coursesProvider.joinAction(info, mode, this)
    }
  }
}

private class CenteredIcon : AsyncProcessIcon.Big("Loading") {
  override fun calculateBounds(container: JComponent): Rectangle {
    val size = container.size
    val iconSize = preferredSize
    return Rectangle((size.width - iconSize.width) / 2, (size.height - iconSize.height) / 2, iconSize.width, iconSize.height)
  }
}
