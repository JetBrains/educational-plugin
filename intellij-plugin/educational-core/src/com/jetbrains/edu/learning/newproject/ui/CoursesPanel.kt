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
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.CoursesDownloadingException
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.newproject.ui.filters.CoursesSearchComponent
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import java.awt.BorderLayout
import java.awt.Rectangle
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
  protected val coursesSearchComponent: CoursesSearchComponent = CoursesSearchComponent(
    getEmptySearchText(),
    { coursesGroups },
    { groups -> updateModel(groups, selectedCourse) }
  )
  private val coursesListDecorator = CoursesListDecorator(this.createCoursesListPanel(), this.tabDescription(), this.toolbarAction())

  private val cardLayout = JBCardLayout()
  protected val coursesGroups = mutableListOf<CoursesGroup>()

  @Volatile
  private var loadingFinished = false

  val languageSettings get() = coursePanel.languageSettings
  val selectedCourse get() = coursesListDecorator.getSelectedCourse()

  init {
    layout = cardLayout
    background = SelectCourseBackgroundColor
    coursesListDecorator.setSelectionListener { this.processSelectionChanged() }

    this.add(createContentPanel(), CONTENT_CARD_NAME)
    this.add(createLoadingPanel(), LOADING_CARD_NAME)
    this.add(createNoCoursesPanel(), NO_COURSES)
    showProgressState()
  }

  open fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    updateModel(coursesGroups, null, true)
  }

  private fun getEmptySearchText(): String = EduCoreBundle.message("course.dialog.search.placeholder")

  open fun createNoCoursesPanel(): JPanel {
    val emptyTextPanel = JBPanelWithEmptyText()
    setNoCoursesPanelDefaultText(emptyTextPanel)

    return emptyTextPanel
  }

  open fun showErrorMessage(e: CoursesDownloadingException) {}

  protected open fun createContentPanel(): JPanel {
    val mainPanel = JPanel(BorderLayout()).apply {
      add(coursesSearchComponent, BorderLayout.PAGE_START)
      add(createSplitPane(), BorderLayout.CENTER)
      background = SelectCourseBackgroundColor
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

  private fun createLoadingPanel() = JPanel(BorderLayout()).apply {
    add(CenteredIcon(), BorderLayout.CENTER)
  }

  protected open fun setNoCoursesPanelDefaultText(panel: JBPanelWithEmptyText) {
    val text = panel.emptyText
    text.text = EduCoreBundle.message("course.dialog.no.courses", ApplicationNamesInfo.getInstance().fullProductName)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide1") + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    @Suppress("DialogTitleCapitalization") // it's ok to start from lowercase as it's the second part of a sentence
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide2"),
                             SimpleTextAttributes.LINK_ATTRIBUTES
    ) { EduBrowser.getInstance().browse(EduNames.NO_COURSES_URL) }
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
    override fun joinCourseAction(info: CourseCreationInfo, mode: CourseMode) {
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
