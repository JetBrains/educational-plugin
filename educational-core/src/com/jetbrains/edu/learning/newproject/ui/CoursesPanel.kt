package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.ui.JBCardLayout
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.supportedTechnologies
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseInfo
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseMode
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CoursePanel
import com.jetbrains.edu.learning.newproject.ui.coursePanel.MAIN_BG_COLOR
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import com.jetbrains.edu.learning.newproject.ui.filters.CoursesFilterComponent
import com.jetbrains.edu.learning.newproject.ui.filters.HumanLanguageFilterDropdown
import com.jetbrains.edu.learning.newproject.ui.filters.ProgrammingLanguageFilterDropdown
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.JPanel


private const val CONTENT_CARD_NAME = "CONTENT"
private const val LOADING_CARD_NAME = "PROGRESS"
private const val NO_COURSES = "NO_COURSES"

abstract class CoursesPanel(private val coursesProvider: CoursesPlatformProvider, private val scope: CoroutineScope) : JPanel() {
  protected var coursePanel: CoursePanel = DialogCoursePanel()
  private val coursesListPanel = this.createCoursesListPanel()
  private val coursesListDecorator = CoursesListDecorator(coursesListPanel, this.tabInfo(), this.toolbarAction())
  protected lateinit var programmingLanguagesFilterDropdown: ProgrammingLanguageFilterDropdown
  protected lateinit var humanLanguagesFilterDropdown: HumanLanguageFilterDropdown
  private val coursesFilterComponent: CoursesFilterComponent = CoursesFilterComponent({ coursesGroups },
                                                                                      { groups -> updateModel(groups, null) })
  private val cardLayout = JBCardLayout()
  protected val coursesGroups = mutableListOf<CoursesGroup>()

  @Volatile
  private var loadingFinished = false

  val languageSettings get() = coursePanel.languageSettings

  val selectedCourse get() = coursesListPanel.selectedCourse

  val locationString: String
    get() {
      // We use `coursePanel` with location field
      // so `coursePanel.locationString` must return not null value
      return coursePanel.locationString!!
    }

  init {
    layout = cardLayout
    background = MAIN_BG_COLOR
    coursesListPanel.setSelectionListener { this.processSelectionChanged() }

    this.add(createContentPanel(), CONTENT_CARD_NAME)
    this.add(createLoadingPanel(), LOADING_CARD_NAME)
    this.add(this.createNoCoursesPanel(), NO_COURSES)
    showProgressState()
  }

  open fun updateModelAfterCourseDeletedFromStorage(deletedCourse: Course) {
    updateModel(coursesGroups, null, true)
  }

  fun hideLoginPanel() = coursesListDecorator.hideLoginPanel()

  private fun createContentPanel(): JPanel {
    val mainPanel = JPanel(BorderLayout())
    mainPanel.add(createAndBindSearchComponent(), BorderLayout.NORTH)
    mainPanel.add(createSplitPane(), BorderLayout.CENTER)
    mainPanel.background = MAIN_BG_COLOR
    return mainPanel
  }

  suspend fun loadCourses() {
    coursesGroups.addAll(withContext(Dispatchers.IO) {
      coursesProvider.loadCourses()
    })

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

  protected open fun tabInfo(): TabInfo? = null

  private fun createLoadingPanel() = JPanel(BorderLayout()).apply {
    add(CenteredIcon(), BorderLayout.CENTER)
  }

  protected open fun createNoCoursesPanel(): JPanel {
    val panel = JBPanelWithEmptyText()
    val text = panel.emptyText
    text.text = EduCoreBundle.message("course.dialog.no.courses", ApplicationNamesInfo.getInstance().fullProductName)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide1") + " ", SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    @Suppress("DialogTitleCapitalization") // it's ok to start from lowercase as it's the second part of a sentence
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide2"),
                             SimpleTextAttributes.LINK_ATTRIBUTES,
                             ActionListener { EduBrowser.getInstance().browse(EduNames.NO_COURSES_URL) })
    return panel
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
    val courses = coursesGroups.flatMap { it.courses }
    humanLanguagesFilterDropdown.updateItems(humanLanguages(courses))
    programmingLanguagesFilterDropdown.updateItems(programmingLanguages(courses))
  }

  protected open fun createCoursesListPanel(): CoursesListPanel = CoursesListWithResetFilters()

  open fun processSelectionChanged() {
    val course = selectedCourse
    if (course != null) {
      coursePanel.bindCourse(course)?.addSettingsChangeListener { doValidation() }
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

  private fun filterCourses(courses: List<Course>): List<Course> {
    var filteredCourses = programmingLanguagesFilterDropdown.filter(courses)
    filteredCourses = humanLanguagesFilterDropdown.filter(filteredCourses)
    return filteredCourses
  }

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?, filterCourses: Boolean = true) {
    if (filterCourses) {
      val filteredCoursesGroups = coursesGroups.map {
        CoursesGroup(it.name, filterCourses(it.courses))
      }
      coursesListPanel.updateModel(filteredCoursesGroups, courseToSelect)
    }
    else {
      coursesListPanel.updateModel(coursesGroups, courseToSelect)
    }
  }

  fun setButtonsEnabled(canStartCourse: Boolean) {
    coursePanel.setButtonsEnabled(canStartCourse)
  }

  private fun humanLanguages(courses: List<Course>): Set<String> = courses.map { it.humanLanguage }.toSet()

  private fun programmingLanguages(courses: List<Course>): Set<String> = courses.map { it.supportedTechnologies }.flatten().toSet()

  private fun createAndBindSearchComponent(): JPanel {
    val searchPanel = JPanel(BorderLayout())
    coursePanel.bindSearchField(coursesFilterComponent)
    searchPanel.add(coursesFilterComponent, BorderLayout.CENTER)

    programmingLanguagesFilterDropdown = ProgrammingLanguageFilterDropdown(programmingLanguages(emptyList())) {
      updateModel(coursesGroups, selectedCourse)
    }
    humanLanguagesFilterDropdown = HumanLanguageFilterDropdown(humanLanguages(emptyList())) {
      updateModel(coursesGroups, selectedCourse)
    }
    val filtersPanel = JPanel(HorizontalLayout(0))
    filtersPanel.add(programmingLanguagesFilterDropdown)
    filtersPanel.add(humanLanguagesFilterDropdown)

    searchPanel.add(filtersPanel, BorderLayout.LINE_END)
    searchPanel.border = JBUI.Borders.empty(11, 0)

    UIUtil.setBackgroundRecursively(searchPanel, MAIN_BG_COLOR)

    return searchPanel
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
      coursesListPanel.setSelectedValue(courseToSelect)
    }
  }

  protected open fun isLoginNeeded() = false

  open inner class CoursesListWithResetFilters : CoursesListPanel() {

    override fun resetFilters() {
      coursesFilterComponent.resetSearchField()
      resetSelection()
      updateModel(coursesGroups, null, true)
    }

    private fun resetSelection() {
      humanLanguagesFilterDropdown.resetSelection()
      programmingLanguagesFilterDropdown.resetSelection()
    }
  }

  private inner class DialogCoursePanel : CoursePanel(true) {
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
