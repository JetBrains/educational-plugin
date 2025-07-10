package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.LanguageSettings.SettingsChangeListener
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.CourseCreationInfo
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.newproject.ui.errors.*
import org.jetbrains.annotations.VisibleForTesting
import java.awt.CardLayout
import java.awt.Component
import java.awt.FlowLayout
import java.io.File
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.DocumentEvent

const val HORIZONTAL_MARGIN = 20
const val DESCRIPTION_AND_SETTINGS_TOP_OFFSET = 23

private const val EMPTY = "empty"
private const val CONTENT = "content"
private const val ERROR_TOP_GAP = 17
private const val ERROR_RIGHT_GAP = 19
private const val ERROR_PANEL_MARGIN = 10
private const val DEFAULT_BUTTON_OFFSET = 3

abstract class CoursePanel(parentDisposable: Disposable, isLocationFieldNeeded: Boolean) : JPanel() {
  protected val tagsPanel: TagsPanel = TagsPanel()
  protected val titlePanel = CourseNameHtmlPanel().apply {
    border = JBUI.Borders.empty(8, HORIZONTAL_MARGIN, 0, 0)
  }
  protected val authorsPanel = AuthorsPanel()
  protected val errorComponent = ErrorComponent(ErrorStateHyperlinkListener(parentDisposable),
                                                ERROR_PANEL_MARGIN) { doValidation() }.apply {
    border = JBUI.Borders.empty(ERROR_TOP_GAP, HORIZONTAL_MARGIN, 0, ERROR_RIGHT_GAP)
  }

  @VisibleForTesting
  val buttonsPanel: ButtonsPanel = ButtonsPanel().apply {
    setStartButtonText(startButtonText(null))
    setOpenButtonText(openButtonText)
    border = JBUI.Borders.emptyTop(18)
  }

  @Suppress("LeakingThis")
  protected val courseDetailsPanel = createCourseDetailsPanel()
  protected val settingsPanel: CourseSettingsPanel = CourseSettingsPanel(parentDisposable, isLocationFieldNeeded).apply {
    background = SelectCourseBackgroundColor
  }
  protected val content = ContentPanel()

  private var courseData: CourseBindData? = null

  var errorState: ErrorState = ErrorState.NothingSelected
  val course: Course? get() = courseData?.course

  private val settingsChangeListener: SettingsChangeListener = SettingsChangeListener { doValidation() }

  protected open val openButtonText: String
    get() = EduCoreBundle.message("course.dialog.open.button")

  val locationString: String?
    get() = settingsPanel.locationString

  val projectSettings: EduProjectSettings?
    get() = settingsPanel.getProjectSettings()

  val languageSettings: LanguageSettings<*>?
    get() = settingsPanel.languageSettings

  init {
    layout = CardLayout()
    border = JBUI.Borders.customLine(DIVIDER_COLOR, 0, 0, 0, 0)

    layoutComponents()
    doValidation()
    setButtonsEnabled(canStartCourse())

    val connection = ApplicationManager.getApplication()
      .messageBus
      .connect(parentDisposable)

    connection.subscribe(DynamicPluginListener.TOPIC, PluginListener())
    connection.subscribe(ApplicationActivationListener.TOPIC, object : ApplicationActivationListener {
      override fun applicationActivated(ideFrame: IdeFrame) {
        doValidation()
      }
    })
  }

  private fun layoutComponents() {
    val emptyStatePanel = JBPanelWithEmptyText().withEmptyText(EduCoreBundle.message("course.dialog.no.course.selected"))
    add(emptyStatePanel, EMPTY)

    addComponents()

    val scrollPane = JBScrollPane(content, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                  ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
      border = JBUI.Borders.empty()
    }

    add(scrollPane, CONTENT)
  }

  protected open fun startButtonText(course: Course?): String {
    return EduCoreBundle.message("course.dialog.start.button")
  }

  protected open fun addComponents() {
    with(content) {
      add(tagsPanel)
      add(titlePanel)
      add(authorsPanel)
      add(errorComponent)
      add(buttonsPanel)
      add(courseDetailsPanel)
      add(settingsPanel)
    }

    buttonsPanel.border = JBUI.Borders.emptyTop(17)
  }

  protected abstract fun joinCourseAction(info: CourseCreationInfo, mode: CourseMode)

  /**
   * In case the user opens an existing course project with the "Open" button, this metadata will be processed after opening
   * the project.
   */
  protected open fun openCourseMetadata(): Map<String, String> = emptyMap()

  protected open fun createCourseDetailsPanel(): NonOpaquePanel = CourseDetailsPanel(HORIZONTAL_MARGIN)

  fun doValidation() {
    setError(getErrorState(course) { validateSettings(it) })
    setButtonsEnabled(errorState.courseCanBeStarted)
  }

  open fun validateSettings(it: Course) = settingsPanel.validateSettings(it)

  fun bindCourse(course: Course): LanguageSettings<*>? = bindCourse(CourseBindData(course))

  fun bindCourse(data: CourseBindData): LanguageSettings<*>? {
    courseData = data
    (layout as CardLayout).show(this, CONTENT)

    buttonsPanel.apply {
      setStartButtonText(startButtonText(course))
    }
    content.update(data)

    doValidation()

    revalidate()
    repaint()

    languageSettings?.addSettingsChangeListener(settingsChangeListener)

    return languageSettings
  }

  fun showEmptyState() {
    (layout as CardLayout).show(this, EMPTY)
  }

  fun setError(errorState: ErrorState) {
    this.errorState = errorState
    setButtonsEnabled(errorState.courseCanBeStarted)
    buttonsPanel.setButtonToolTip(null)
    hideErrorPanel()

    showError(errorState)
  }

  private fun setError(validationMessage: ValidationMessage) {
    errorComponent.setErrorMessage(validationMessage)
    buttonsPanel.setButtonToolTip(validationMessage.message)
  }

  protected open fun showError(errorState: ErrorState) {
    if (errorState is ErrorState.LocationError) {
      addOneTimeLocationFieldValidation()
    }

    val message = errorState.message ?: return
    when (errorState) {
      is ErrorState.HyperskillLoginNeeded -> {
        errorComponent.setErrorMessage(message)
        buttonsPanel.setButtonToolTip(EduCoreBundle.message("course.dialog.login.required"))
      }
      is ErrorState.LoginRequired -> {
        course?.let {
          if (CoursesStorage.getInstance().hasCourse(it)) {
            return
          }
        }
        setError(message)
      }
      else -> {
        setError(message)
      }
    }
    showErrorPanel()
  }

  private fun addOneTimeLocationFieldValidation() {
    settingsPanel.addLocationFieldDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
        settingsPanel.removeLocationFieldDocumentListener(this)
      }
    })
  }

  private fun showErrorPanel() {
    errorComponent.isVisible = true
    revalidate()
    repaint()
  }

  fun hideErrorPanel() {
    errorComponent.isVisible = false
    revalidate()
    repaint()
  }

  fun setButtonsEnabled(canStartCourse: Boolean) {
    buttonsPanel.setButtonsEnabled(canStartCourse)
  }

  private fun canStartCourse(): Boolean = errorState.courseCanBeStarted

  private fun joinCourse(course: Course, courseMode: CourseMode) {
    val currentLocation = locationString
    val locationErrorState = when {
      // if it's null it means there's no location field, and it's ok
      currentLocation == null -> ErrorState.None
      currentLocation.isEmpty() -> ErrorState.EmptyLocation
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(currentLocation))) -> ErrorState.InvalidLocation
      else -> ErrorState.None
    }
    if (locationErrorState != ErrorState.None) {
      setError(locationErrorState)
    }
    else {
      val courseInfo = CourseCreationInfo(course, currentLocation, languageSettings?.getSettings())
      joinCourseAction(courseInfo, courseMode)
    }
  }

  @VisibleForTesting
  inner class ButtonsPanel : NonOpaquePanel(), CourseSelectionListener {
    @VisibleForTesting
    val buttons: List<CourseButtonBase> = listOf(
      StartCourseButton(joinCourse = { course, courseMode -> joinCourse(course, courseMode) }),
      OpenCourseButton{ openCourseMetadata() }
    )

    init {
      val contentPanel = NonOpaquePanel().apply {
        layout = FlowLayout(FlowLayout.LEFT, 0, 0)
        border = JBUI.Borders.emptyLeft(HORIZONTAL_MARGIN - DEFAULT_BUTTON_OFFSET)
      }
      buttons.forEach {
        contentPanel.add(it)
      }

      add(contentPanel)
    }

    fun setStartButtonText(text: String) {
      buttons.first().text = text
    }

    fun setOpenButtonText(text: String) {
      buttons[1].text = text
    }

    override fun onCourseSelectionChanged(data: CourseBindData) {
      buttons.forEach {
        it.update(data.course)
      }
    }

    fun setButtonToolTip(tooltip: String?) {
      buttons.forEach {
        it.toolTipText = tooltip
      }
    }

    fun setButtonsEnabled(isEnabled: Boolean) {
      buttons.forEach {
        it.isEnabled = isEnabled
      }
    }
  }

  companion object {
    // default divider's color too dark in Darcula, so use the same color as in plugins dialog
    val DIVIDER_COLOR = JBColor(0xC5C5C5, 0x515151)
  }

  protected class ContentPanel : NonOpaquePanel() {
    init {
      layout = VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false)
      background = SelectCourseBackgroundColor
      border = JBUI.Borders.emptyRight(HORIZONTAL_MARGIN)
    }

    override fun add(comp: Component?): Component {
      if (comp !is CourseSelectionListener) {
        error("Content of this panel is updatable, so component must implement `Updatable`")
      }
      return super.add(comp)
    }

    fun update(data: CourseBindData) {
      // we have to update settings prior to buttons
      components.forEach {
        (it as CourseSelectionListener).onCourseSelectionChanged(data)
      }
    }
  }

  private inner class PluginListener : DynamicPluginListener {
    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
      rebindCourse()
    }

    override fun pluginUnloaded(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
      rebindCourse()
    }

    private fun rebindCourse() {
      val data = courseData ?: return
      invokeLater {
        bindCourse(data)
      }
    }
  }
}
