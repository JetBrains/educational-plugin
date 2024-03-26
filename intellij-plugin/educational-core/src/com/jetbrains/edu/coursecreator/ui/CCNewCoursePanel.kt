package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.BrowserUtil
import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.util.whenItemSelected
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.wm.IdeFrame
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.text.nullize
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.feedback.CCInIdeFeedbackDialog
import com.jetbrains.edu.coursecreator.getDefaultCourseType
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.configuration.EducationalExtensionPoint
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import com.jetbrains.edu.learning.courseFormat.ext.technologyName
import com.jetbrains.edu.learning.enablePlugins
import com.jetbrains.edu.learning.feedback.CourseFeedbackInfoData
import com.jetbrains.edu.learning.getDisabledPlugins
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.edu.learning.newproject.ui.courseSettings.CourseSettingsPanel
import com.jetbrains.edu.learning.newproject.ui.errors.*
import org.jdesktop.swingx.HorizontalLayout
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import java.awt.Component
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

class CCNewCoursePanel(
  private val parentDisposable: Disposable,
  course: Course? = null,
  courseProducer: () -> Course = ::EduCourse,
) : JPanel() {
  private val titleField: CourseTitleField = CourseTitleField()
  private val descriptionTextArea: JTextArea = JTextArea()

  private val settings: CourseSettingsPanel = CourseSettingsPanel(parentDisposable)
  private val pathField: PathField = PathField()
  private val locationField: LabeledComponent<TextFieldWithBrowseButton> = createLocationField()
  private lateinit var languageSettings: LanguageSettings<*>
  private var requiredAndDisabledPlugins: List<PluginId> = emptyList()
  private var validationListener: ValidationListener? = null

  private val errorComponent = ErrorComponent(getHyperlinkListener()) { doValidation() }

  private val context: UserDataHolder = UserDataHolderBase()
  private var languageSettingsDisposable: CheckedDisposable? = null

  private val _course: Course
  val course: Course
    get() {
      _course.name = titleField.text
      _course.description = descriptionTextArea.text
      _course.isMarketplace = true
      _course.marketplaceCourseVersion = 1
      return _course
    }


  val projectSettings: EduProjectSettings get() = languageSettings.getSettings()
  val locationString: String get() = locationField.component.text

  init {
    layout = BorderLayout()
    // Check both Darcula and Light theme before changing size
    preferredSize = JBUI.size(700, 372)
    minimumSize = JBUI.size(700, 372)

    _course = (course ?: courseProducer()).apply { courseMode = CourseMode.EDUCATOR }

    descriptionTextArea.rows = 10
    descriptionTextArea.lineWrap = true
    descriptionTextArea.wrapStyleWord = true

    titleField.document = CourseTitleDocument()
    titleField.complementaryTextField = pathField
    pathField.complementaryTextField = titleField

    val bottomPanel = JPanel(BorderLayout())
    errorComponent.minimumSize = JBUI.size(700, 34)
    errorComponent.preferredSize = errorComponent.minimumSize
    errorComponent.border = JBUI.Borders.empty(5, 6, 2, 2)
    bottomPanel.add(errorComponent, BorderLayout.SOUTH)
    bottomPanel.add(settings, BorderLayout.NORTH)

    val courseData = collectCoursesData(course)
    val defaultCourseType = getDefaultCourseType(courseData)

    val topPanel = panel {
      row(EduCoreBundle.message("cc.new.course.title")) {
        cell(titleField)
          .align(AlignX.FILL)
      }
      row(EduCoreBundle.message("cc.new.course.type")) {
        comboBox(courseData, CourseDataRenderer())
          .enabled(course == null)
          .align(AlignX.FILL)
          .applyToComponent {
            whenItemSelected {
              onCourseDataSelected(it)
            }
          }
          .bindItem({ defaultCourseType }, {})
      }
      row(EduCoreBundle.message("cc.new.course.description")) {
        scrollCell(descriptionTextArea)
          .align(AlignX.FILL)
      }
      val feedbackPanel = createFeedbackPanel(titleField, _course)
      row {
        cell(feedbackPanel).align(AlignX.RIGHT)
      }
    }
    add(topPanel, BorderLayout.NORTH)
    add(bottomPanel, BorderLayout.SOUTH)

    if (defaultCourseType != null) {
      onCourseDataSelected(defaultCourseType)
    }

    setupValidation()

    if (course != null) {
      descriptionTextArea.text = course.description
      titleField.setTextManually(course.name)
    }

    ApplicationManager
      .getApplication()
      .messageBus
      .connect(parentDisposable)
      .subscribe(ApplicationActivationListener.TOPIC, object : ApplicationActivationListener {
        override fun applicationActivated(ideFrame: IdeFrame) {
          doValidation()
        }
      })
  }

  fun setValidationListener(listener: ValidationListener?) {
    validationListener = listener
    doValidation()
  }

  private fun setupValidation() {
    val validator = object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
      }
    }

    titleField.document.addDocumentListener(validator)
    descriptionTextArea.document.addDocumentListener(validator)
    locationField.component.textField.document.addDocumentListener(validator)
  }

  private fun getHyperlinkListener(): HyperlinkListener = HyperlinkListener { e ->
    if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
      if (requiredAndDisabledPlugins.isNotEmpty()) enablePlugins(requiredAndDisabledPlugins)
      else errorComponent.validationMessageLink?.let { BrowserUtil.browse(it) }
    }
  }

  fun validateLocation() {
    val validationMessage = when {
      locationString.isBlank() -> ValidationMessage(EduCoreBundle.message("error.enter.location"))
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) -> ValidationMessage(
        EduCoreBundle.message("error.wrong.location"))
      else -> null
    }
    processValidationResult(SettingsValidationResult.Ready(validationMessage))
  }

  private fun doValidation() {
    val excessTitleLength = titleField.text.length - MAX_COURSE_TITLE_LENGTH
    val settingsValidationResult = when {
      titleField.text.isNullOrBlank() -> ValidationMessage(EduCoreBundle.message("cc.new.course.error.enter.title")).ready()
      excessTitleLength > 0 -> ValidationMessage(
        EduCoreBundle.message("cc.new.course.error.exceeds.max.title.length", excessTitleLength, MAX_COURSE_TITLE_LENGTH)).ready()
      descriptionTextArea.text.isNullOrBlank() -> ValidationMessage(EduCoreBundle.message("cc.new.course.error.enter.description")).ready()
      requiredAndDisabledPlugins.isNotEmpty() -> ErrorState.errorMessage(requiredAndDisabledPlugins).ready()
      else -> languageSettings.validate(null, locationString)
    }
    processValidationResult(settingsValidationResult)
  }

  private fun processValidationResult(settingsValidationResult: SettingsValidationResult) {
    when (settingsValidationResult) {
      is SettingsValidationResult.Pending -> {
        validationListener?.onInputDataValidated(false)
      }
      is SettingsValidationResult.Ready -> {
        val validationMessage = settingsValidationResult.validationMessage
        if (validationMessage != null) {
          errorComponent.setErrorMessage(validationMessage)
          errorComponent.isVisible = true
          settings.setOn(true)
          revalidate()
        }
        else {
          errorComponent.isVisible = false
        }
        validationListener?.onInputDataValidated(validationMessage == null)
      }
    }
  }

  private fun createLocationField(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton(pathField)
    field.addBrowseFolderListener(EduCoreBundle.message("cc.new.course.select.location.title"),
                                  EduCoreBundle.message("cc.new.course.select.location.description"), null,
                                  FileChooserDescriptorFactory.createSingleFolderDescriptor())
    return LabeledComponent.create(field, "${EduCoreBundle.message("cc.new.course.select.location.label")}:", BorderLayout.WEST)
  }

  private fun onCourseDataSelected(courseData: CourseData) {
    languageSettingsDisposable?.let(Disposer::dispose)
    val settingsDisposable = Disposer.newCheckedDisposable(parentDisposable, "languageSettingsDisposable")
    languageSettingsDisposable = settingsDisposable

    val courseName = "${courseData.displayName.replaceFirstChar { it.titlecaseChar() }.replace(File.separatorChar, '_')} ${
      EduCoreBundle.message("item.course.title")
    }"
    _course.name = courseName
    val file = FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), courseName, "")
    if (!titleField.isChangedByUser) {
      titleField.setTextManually(file.name)
      if (!pathField.isChangedByUser) {
        pathField.setTextManually(file.absolutePath)
      }
    }

    val configurator = EduConfiguratorManager.findConfigurator(courseData.courseType, courseData.environment,
                                                               courseData.language) ?: return
    _course.languageId = courseData.language.id
    _course.environment = courseData.environment
    languageSettings = configurator.courseBuilder.getLanguageSettings()
    languageSettings.addSettingsChangeListener { doValidation() }

    val settings = arrayListOf<LabeledComponent<*>>(locationField)
    settings.addAll(languageSettings.getLanguageSettingsComponents(_course, settingsDisposable, context))
    this.settings.setSettingsComponents(settings)

    requiredAndDisabledPlugins = getDisabledPlugins(configurator.pluginRequirements)
    descriptionTextArea.text = _course.description.nullize() ?: getCreateCourseDialogMessage()
    doValidation()
  }

  @Nls
  private fun getCreateCourseDialogMessage(): String {
    return EduCoreBundle.message("dialog.message.create.course", _course.technologyName ?: _course.languageDisplayName)
  }

  private fun collectCoursesData(course: Course?): List<CourseData> {
    val courseData = if (course != null) {
      listOfNotNull(obtainCourseData(course.languageId, course.environment, course.itemType))
    }
    else {
      EduConfiguratorManager.allExtensions()
        .filter { it.instance.isCourseCreatorEnabled }
        .filter { it.courseType == _course.itemType }.mapNotNull { extension -> obtainCourseData(extension) }
    }
    return courseData.sortedBy { it.displayName }
  }

  private fun obtainCourseData(languageId: String, environment: String, courseType: String): CourseData? {
    val language = getLanguageById(languageId) ?: return null
    val extension = EduConfiguratorManager.findExtension(courseType, environment, language) ?: return null
    return obtainCourseData(extension, language)
  }

  private fun obtainCourseData(
    extension: EducationalExtensionPoint<EduConfigurator<*>>,
    language: Language? = getLanguageById(extension.language),
  ): CourseData? {
    if (language == null) return null
    val environment = extension.environment
    val courseType = extension.courseType
    val displayName = extension.displayName ?: run {
      when (courseType) {
        EduFormatNames.PYCHARM -> if (environment == DEFAULT_ENVIRONMENT) language.displayName else "${language.displayName} ($environment)"
        else -> "$courseType ${language.displayName}"
      }
    }

    return CourseData(language, courseType, environment, displayName, extension.instance.logo)
  }

  private fun getLanguageById(languageId: String): Language? {
    return Language.findLanguageByID(languageId) ?: run {
      LOG.info("Language with id $languageId not found")
      null
    }
  }

  private fun createFeedbackPanel(courseTitleField: CourseTitleField, course: Course): JPanel {
    val panel = JPanel(HorizontalLayout())
    val message = JLabel(EduCoreBundle.message("ui.feedback.cc.label"))
    message.apply {
      border = JBUI.Borders.emptyRight(3)
      foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
    }

    val hyperlinkLabel = HyperlinkLabel(EduCoreBundle.message("ui.feedback.cc.hyperlink.label"))

    hyperlinkLabel.addHyperlinkListener {
      val dialog = CCInIdeFeedbackDialog(CourseFeedbackInfoData.from(course, courseTitleField.text))
      dialog.showAndGet()
    }
    panel.add(message)
    panel.add(hyperlinkLabel)
    return panel
  }

  companion object {
    private val LOG = Logger.getInstance(CCNewCoursePanel::class.java)
    // max course title length according to https://github.com/JetBrains/intellij-plugin-verifier/pull/67/files
    private const val MAX_COURSE_TITLE_LENGTH = 64
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  data class CourseData(
    val language: Language,
    val courseType: String,
    val environment: String,
    val displayName: String,
    val icon: Icon?,
  )

  private class CourseTitleDocument : PlainDocument() {
    override fun insertString(offs: Int, str: String?, a: AttributeSet?) {
      if (str == null || str.none { it in ILLEGAL_CHARS }) {
        super.insertString(offs, str, a)
      }
    }

    companion object {
      private val ILLEGAL_CHARS = arrayOf(File.separatorChar, '/', '|', ':')
    }
  }

  private class PathField : CCSyncTextField() {
    override fun doSync(complementaryTextField: CCSyncTextField) {
      val path = text ?: return
      val lastSeparatorIndex = path.lastIndexOf(File.separator)
      if (lastSeparatorIndex >= 0 && lastSeparatorIndex + 1 < path.length) {
        complementaryTextField.setTextManually(path.substring(lastSeparatorIndex + 1))
      }
    }
  }

  class CourseTitleField : CCSyncTextField() {
    override fun doSync(complementaryTextField: CCSyncTextField) {
      val courseName = text ?: return
      val path = complementaryTextField.text?.trim() ?: return
      val lastSeparatorIndex = path.lastIndexOf(File.separator)
      if (lastSeparatorIndex >= 0) {
        complementaryTextField.setTextManually(path.substring(0, lastSeparatorIndex + 1) + courseName)
      }
    }
  }

  private class CourseDataRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
      list: JList<*>,
      value: Any?,
      index: Int,
      isSelected: Boolean,
      cellHasFocus: Boolean
    ): Component {
      val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
      if (component is JLabel && value is CourseData) {
        component.text = value.displayName
        component.icon = value.icon
      }
      return component
    }
  }
}
