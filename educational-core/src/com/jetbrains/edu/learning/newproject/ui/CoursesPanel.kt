package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.ui.FilterComponent
import com.intellij.ui.JBCardLayout
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.ErrorState.*
import com.jetbrains.edu.learning.newproject.ui.ErrorState.Companion.forCourse
import com.jetbrains.edu.learning.newproject.ui.coursePanel.NewCoursePanel
import com.jetbrains.edu.learning.newproject.ui.filters.HumanLanguageFilterDropdown
import com.jetbrains.edu.learning.newproject.ui.filters.ProgrammingLanguageFilterDropdown
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import java.awt.Rectangle
import java.awt.event.ActionListener
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import kotlin.collections.HashSet


private const val CONTENT_CARD_NAME = "CONTENT"
private const val LOADING_CARD_NAME = "PROGRESS"
private const val NO_COURSES = "NO_COURSES"

abstract class CoursesPanel(coursesProvider: CoursesPlatformProvider) : JPanel() {
  protected var coursePanel: NewCoursePanel = NewCoursePanel(isStandalonePanel = false, isLocationFieldNeeded = true)
  private var courses: MutableList<Course> = mutableListOf()
  protected lateinit var coursesListPanel: CoursesListPanel
  private lateinit var myProgrammingLanguagesFilterDropdown: ProgrammingLanguageFilterDropdown
  private lateinit var myHumanLanguagesFilterDropdown: HumanLanguageFilterDropdown
  private val cardLayout = JBCardLayout()

  val projectSettings get() = coursePanel.projectSettings

  val selectedCourse get() = coursesListPanel.selectedCourse

  val locationString: String
    get() {
      // We use `coursePanel` with location field
      // so `coursePanel.locationString` must return not null value
      return coursePanel.locationString!!
    }

  init {
    layout = cardLayout

    addCourseValidationListener(object : CourseValidationListener {
      override fun validationStatusChanged(canStartCourse: Boolean) {
        coursePanel.setButtonsEnabled(canStartCourse)
      }
    })

    this.add(createContentPanel(coursesProvider), CONTENT_CARD_NAME)
    this.add(createLoadingPanel(), LOADING_CARD_NAME)
    this.add(createNoCoursesPanel(), NO_COURSES)
    showProgressState()

    coursesListPanel.addListener { processSelectionChanged() }
  }

  fun hideLoginPanel() = coursesListPanel.hideLoginPanel()

  private fun createContentPanel(coursesProvider: CoursesPlatformProvider): JPanel {
    val mainPanel = JPanel(BorderLayout())
    mainPanel.add(createAndBindSearchComponent(), BorderLayout.NORTH)
    mainPanel.add(createSplitPane(coursesProvider), BorderLayout.CENTER)
    return mainPanel
  }

  suspend fun loadCourses() {
    courses.addAll(coursesListPanel.loadCourses())
    updateFilters()
    updateModel(courses, coursesListPanel.selectedCourse)
    showContent(courses.isEmpty())
    processSelectionChanged()
  }

  private fun createSplitPane(coursesProvider: CoursesPlatformProvider): JPanel {
    coursesListPanel = CoursesListPanel(toolbarAction(), coursesProvider, tabInfo())
    coursesListPanel.addListener { processSelectionChanged() }

    val splitPane = OnePixelSplitter()
    splitPane.firstComponent = coursesListPanel
    splitPane.secondComponent = coursePanel
    splitPane.proportion = 0.46f

    val splitPaneRoot = JPanel(BorderLayout()) // needed to set borders
    splitPaneRoot.add(splitPane, BorderLayout.CENTER)
    splitPaneRoot.border = JBUI.Borders.customLine(NewCoursePanel.DIVIDER_COLOR, 1, 0, 0, 0)
    return splitPaneRoot
  }

  protected open fun toolbarAction(): AnAction? = null

  protected open fun tabInfo(): TabInfo? = null

  interface CourseValidationListener {
    fun validationStatusChanged(canStartCourse: Boolean)
  }

  private fun createLoadingPanel() = JPanel(BorderLayout()).apply {
    add(CenteredIcon(), BorderLayout.CENTER)
  }

  private fun createNoCoursesPanel(): JPanel {
    val panel = JBPanelWithEmptyText()
    val text = panel.emptyText
    text.text = EduCoreBundle.message("course.dialog.no.courses", ApplicationNamesInfo.getInstance().fullProductName)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide1", EduNames.NO_COURSES_URL) + " ",
                             SimpleTextAttributes.GRAYED_ATTRIBUTES, null)
    text.appendSecondaryText(EduCoreBundle.message("help.use.guide2", EduNames.NO_COURSES_URL),
                             SimpleTextAttributes.LINK_ATTRIBUTES, ActionListener { BrowserUtil.browse(EduNames.NO_COURSES_URL) })
    return panel
  }

  private fun showProgressState() = cardLayout.show(this, LOADING_CARD_NAME)

  fun showContent(empty: Boolean) {
    if (empty) {
      cardLayout.show(this, NO_COURSES)
      return
    }
    cardLayout.show(this, CONTENT_CARD_NAME)
  }

  private fun updateFilters() {
    myHumanLanguagesFilterDropdown.updateItems(humanLanguages(courses))
    myProgrammingLanguagesFilterDropdown.updateItems(programmingLanguages(courses))
  }

  private fun processSelectionChanged() {
    val course = selectedCourse
    if (course != null) {
      coursePanel.bindCourse(course)?.addSettingsChangeListener { doValidation(course) }
    }
    else {
      coursePanel.showEmptyState()
    }
    doValidation(course)
  }

  fun doValidation(course: Course? = coursesListPanel.selectedCourse) {
    var languageError: ErrorState = NothingSelected
    if (course != null) {
      val languageSettingsMessage = coursePanel.validateSettings(course)
      languageError = languageSettingsMessage?.let { LanguageSettingsError(it) } ?: None
    }
    val errorState = forCourse(course).merge(languageError)
    setError(errorState)
    notifyListeners(errorState.courseCanBeStarted)
  }

  fun setError(errorState: ErrorState) {
    coursePanel.setError(errorState)
  }

  private fun filterCourses(courses: List<Course>): List<Course> {
    var filteredCourses = myProgrammingLanguagesFilterDropdown.filter(courses)
    filteredCourses = myHumanLanguagesFilterDropdown.filter(filteredCourses)
    return filteredCourses
  }

  protected fun updateModel(courses: List<Course>, courseToSelect: Course?, filterCourses: Boolean = true) {
    val coursesToAdd = if (filterCourses) filterCourses(courses) else courses
    coursesListPanel.updateModel(coursesToAdd, courseToSelect)
  }

  private fun addCourseValidationListener(listener: CourseValidationListener) {
    coursePanel.addCourseValidationListener(listener)
  }

  fun notifyListeners(canStartCourse: Boolean) {
    coursePanel.notifyListeners(canStartCourse)
  }

  private fun humanLanguages(courses: List<Course>): Set<String> = courses.map { it.humanLanguage }.toSet()

  private fun programmingLanguages(courses: List<Course>): Set<String> = courses.mapNotNull { it.technologyName }.toSet()

  private fun createAndBindSearchComponent(): JPanel {
    val searchPanel = JPanel(BorderLayout())
    val searchField = LanguagesFilterComponent()
    coursePanel.bindSearchField(searchField)
    searchPanel.add(searchField, BorderLayout.CENTER)

    myProgrammingLanguagesFilterDropdown = ProgrammingLanguageFilterDropdown(programmingLanguages(emptyList())) {
      updateModel(courses, selectedCourse)
    }
    myHumanLanguagesFilterDropdown = HumanLanguageFilterDropdown(humanLanguages(emptyList())) {
      updateModel(courses, selectedCourse)
    }
    val filtersPanel = JPanel(HorizontalLayout(0))
    filtersPanel.add(myProgrammingLanguagesFilterDropdown)
    filtersPanel.add(myHumanLanguagesFilterDropdown)

    searchPanel.add(filtersPanel, BorderLayout.LINE_END)
    searchPanel.border = JBUI.Borders.empty(8, 0)

    return searchPanel
  }

  open fun updateCourseListAfterLogin() {
  }

  inner class LanguagesFilterComponent : FilterComponent("Edu.NewCourse", 5, true) {

    init {
      textEditor.border = null
    }

    override fun filter() {
      val filter = filter
      val filtered = ArrayList<Course>()
      for (course in courses) {
        if (accept(filter, course)) {
          filtered.add(course)
        }
      }
      updateModel(filtered, selectedCourse)
    }

    private fun accept(@NonNls filter: String, course: Course): Boolean {
      if (filter.isEmpty()) {
        return true
      }
      val filterParts = getFilterParts(filter)
      val courseName = course.name.toLowerCase(Locale.getDefault())
      for (filterPart in filterParts) {
        if (courseName.contains(filterPart)) return true
        for (tag in course.tags) {
          if (tag.accept(filterPart)) {
            return true
          }
        }
        for (authorName in course.authorFullNames) {
          if (authorName.toLowerCase(Locale.getDefault()).contains(filterPart)) {
            return true
          }
        }
      }
      return false
    }

    private fun getFilterParts(@NonNls filter: String): Set<String> {
      return HashSet(listOf(*filter.toLowerCase().split(" ".toRegex()).toTypedArray()))
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
