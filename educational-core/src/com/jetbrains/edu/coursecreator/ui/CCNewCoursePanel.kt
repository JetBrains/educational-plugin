package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.Language
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.*
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.enablePlugins
import com.jetbrains.edu.learning.getDisabledPlugins
import com.jetbrains.edu.learning.newproject.ui.AdvancedSettings
import com.jetbrains.edu.learning.newproject.ui.ErrorMessage
import com.jetbrains.edu.learning.newproject.ui.ErrorState
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

class CCNewCoursePanel(course: Course? = null) : JPanel() {

  private val myPanel: JPanel
  private val myCourseTypesComboBox: ComboBox<CourseTypeData> = ComboBox()
  private val myTitleField: CourseTitleField = CourseTitleField()
  private val myAuthorField: JBTextField = JBTextField()
  private val myDescriptionTextArea: JTextArea = JTextArea()

  private val myAdvancedSettings: AdvancedSettings = AdvancedSettings()
  private val myPathField: PathField = PathField()
  private val myLocationField: LabeledComponent<TextFieldWithBrowseButton> = createLocationField()

  private val myErrorLabel = HyperlinkLabel()

  private val myCourse: Course = (course ?: Course()).apply { courseMode = CCUtils.COURSE_MODE }
  private lateinit var myLanguageSettings: LanguageSettings<*>

  private var myRequiredAndDisabledPlugins: List<String> = emptyList()

  private var myValidationListener: ValidationListener? = null

  init {
    layout = BorderLayout()
    preferredSize = JBUI.size(700, 400)
    minimumSize = JBUI.size(700, 400)

    myDescriptionTextArea.rows = 10
    myDescriptionTextArea.lineWrap = true
    myDescriptionTextArea.wrapStyleWord = true

    val scrollPane = JBScrollPane(myDescriptionTextArea)
    myPanel = panel {
      row("Title:") { myTitleField(CCFlags.pushX) }
      row("Instructor:") { myAuthorField() }
      row("Course Type:") { myCourseTypesComboBox(CCFlags.growX) }
      row("Description:") { scrollPane(CCFlags.growX) }
    }

    myTitleField.document = CourseTitleDocument()
    myTitleField.complementaryTextField = myPathField
    myPathField.complementaryTextField = myTitleField

    myErrorLabel.border = JBUI.Borders.emptyTop(8)
    myErrorLabel.foreground = MessageType.ERROR.titleForeground

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.add(myErrorLabel, BorderLayout.SOUTH)
    bottomPanel.add(myAdvancedSettings, BorderLayout.NORTH)

    add(myPanel, BorderLayout.NORTH)
    add(bottomPanel, BorderLayout.SOUTH)

    myErrorLabel.addHyperlinkListener { enablePlugins(myRequiredAndDisabledPlugins) }

    myCourseTypesComboBox.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is CourseTypeData) {
          component.text = value.displayName
          component.icon = value.icon
        }
        return component
      }
    }
    myCourseTypesComboBox.addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        onCourseTypeSelected(e.item as CourseTypeData)
      }
    }

    setupValidation()
    setDefaultValues()
    collectCourseTypes(course)

    if (course != null) {
      myDescriptionTextArea.text = course.description
      myTitleField.setTextManually(course.name)
      myCourseTypesComboBox.isEnabled = false
    }
  }

  val course: Course get() {
    myCourse.name = myTitleField.text
    myCourse.description = myDescriptionTextArea.text
    myCourse.setAuthorsAsString(StringUtil.splitByLines(myAuthorField.text.orEmpty()))

    return myCourse
  }
  val projectSettings: Any get() = myLanguageSettings.settings
  val locationString: String get() = myLocationField.component.text

  fun setValidationListener(listener: ValidationListener?) {
    myValidationListener = listener
    doValidation()
  }

  private fun setupValidation() {
    val validator = object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        doValidation()
      }
    }

    myTitleField.document.addDocumentListener(validator)
    myAuthorField.document.addDocumentListener(validator)
    myDescriptionTextArea.document.addDocumentListener(validator)
    myLocationField.component.textField.document.addDocumentListener(validator)
  }

  private fun doValidation() {
    val errorMessage = when {
      myTitleField.text.isNullOrBlank() -> ErrorMessage("Enter course title")
      myAuthorField.text.isNullOrBlank() -> ErrorMessage("Enter course instructor")
      myDescriptionTextArea.text.isNullOrBlank() -> ErrorMessage("Enter course description")
      locationString.isBlank() -> ErrorMessage("Enter course location")
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) -> ErrorMessage("Can't create course at this location")
      myRequiredAndDisabledPlugins.isNotEmpty() -> ErrorState.errorMessage(myRequiredAndDisabledPlugins)
      else -> {
        val errorMessage = myLanguageSettings.validate()
        if (errorMessage != null) {
          myAdvancedSettings.setOn(true)
          ErrorMessage(errorMessage)
        } else {
          null
        }
      }
    }
    if (errorMessage != null) {
      myErrorLabel.setHyperlinkText(errorMessage.beforeLink, errorMessage.link, errorMessage.afterLink)
    }
    myErrorLabel.isVisible = errorMessage != null
    myValidationListener?.onInputDataValidated(errorMessage == null)
  }

  private fun createLocationField(): LabeledComponent<TextFieldWithBrowseButton> {
    val field = TextFieldWithBrowseButton(myPathField)
    field.addBrowseFolderListener("Select Course Location", "Select course location", null,
                                  FileChooserDescriptorFactory.createSingleFolderDescriptor())
    return LabeledComponent.create(field, "Location:", BorderLayout.WEST)
  }

  private fun setDefaultValues() {
    val userName = System.getProperty("user.name")
    if (userName != null) {
      myAuthorField.text = userName
    }
  }

  private fun onCourseTypeSelected(courseTypeData: CourseTypeData) {
    val courseName = "${courseTypeData.displayName.capitalize()} Course"
    val file = FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), courseName, "")
    if (!myTitleField.isChangedByUser) {
      myTitleField.setTextManually(file.name)
      if (!myPathField.isChangedByUser) {
        myPathField.setTextManually(file.absolutePath)
      }
    }

    val configurator = EduConfiguratorManager.forLanguageAndCourseType(courseTypeData.courseType, courseTypeData.language) ?: return
    myCourse.language = courseTypeData.language.id
    myCourse.courseType = courseTypeData.courseType
    myLanguageSettings = configurator.courseBuilder.languageSettings
    myLanguageSettings.addSettingsChangeListener { doValidation() }

    val settings = arrayListOf<LabeledComponent<*>>(myLocationField)
    settings.addAll(myLanguageSettings.getLanguageSettingsComponents(myCourse))
    myAdvancedSettings.setSettingsComponents(settings)

    myRequiredAndDisabledPlugins = getDisabledPlugins(configurator.pluginRequirements())
    doValidation()
  }

  private fun collectCourseTypes(course: Course?) {
    val courseTypeData = if (course != null) {
      listOfNotNull(obtainCourseTypeData(course.languageID, course.courseType))
    } else {
      EduConfiguratorManager.allExtensions()
        .filter { it.courseCreatorEnabled }
        .mapNotNull { extension -> obtainCourseTypeData(extension.language, extension.courseType) }.toList()
    }
    courseTypeData
      .sortedBy { it.displayName }
      .forEach { myCourseTypesComboBox.addItem(it) }
  }

  private fun obtainCourseTypeData(languageId: String, courseType: String): CourseTypeData? {
    val language = Language.findLanguageByID(languageId)
    if (language == null) {
      LOG.info("Language with id $languageId not found")
      return null
    }
    return CourseTypeData(language, courseType, EduConfiguratorManager.forLanguageAndCourseType(courseType, language)?.logo)
  }

  companion object {
    private val LOG = Logger.getInstance(CCNewCoursePanel::class.java)
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  private data class CourseTypeData(val language: Language, val courseType: String = EduNames.PYCHARM, val icon: Icon?) {
  val displayName get(): String {
      return when (courseType) {
        EduNames.PYCHARM -> language.displayName
        EduNames.ANDROID -> courseType
        else -> "$courseType ${language.displayName}"
      }
    }
  }

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

  private class CourseTitleField : CCSyncTextField() {
    override fun doSync(complementaryTextField: CCSyncTextField) {
      val courseName = text ?: return
      val path = complementaryTextField.text?.trim() ?: return
      val lastSeparatorIndex = path.lastIndexOf(File.separator)
      if (lastSeparatorIndex >= 0) {
        complementaryTextField.setTextManually(path.substring(0, lastSeparatorIndex + 1) + courseName)
      }
    }
  }
}
