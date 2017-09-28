package com.jetbrains.edu.coursecreator.ui

import com.intellij.ide.impl.ProjectUtil
import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.HideableDecorator
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.CCFlags
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduPluginConfigurator
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ItemEvent
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.text.AttributeSet
import javax.swing.text.PlainDocument

class CCNewCoursePanel : JPanel() {

  private val myPanel: JPanel
  private val myLanguageComboBox: ComboBox<LanguageData> = ComboBox()
  private val myTitleField: CourseTitleField = CourseTitleField()
  private val myAuthorField: JBTextField = JBTextField()
  private val myDescriptionTextArea: JTextArea = JTextArea()

  private val myAdvancedSettingsPlaceholder: JPanel = JPanel(BorderLayout())
  private val myAdvancedSettings: JPanel = JPanel(BorderLayout())
  private val myPathField: PathField = PathField()
  private val myLocationField: LabeledComponent<TextFieldWithBrowseButton> = createLocationField()

  private val myErrorLabel = JBLabel()

  private lateinit var mySelectedLanguage: Language

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
      row("Language:") { myLanguageComboBox(CCFlags.growX) }
      row("Description:") { scrollPane(CCFlags.growX) }
    }

    myTitleField.document = CourseTitleDocument()
    myTitleField.complementaryTextField = myPathField
    myPathField.complementaryTextField = myTitleField

    val decorator = HideableDecorator(myAdvancedSettingsPlaceholder, "Advanced Settings", false)
    decorator.setContentComponent(myAdvancedSettings)
    myAdvancedSettings.border = JBUI.Borders.empty(0, IdeBorderFactory.TITLED_BORDER_INDENT, 5, 0)
    myAdvancedSettingsPlaceholder.add(myAdvancedSettings, BorderLayout.CENTER)

    myErrorLabel.border = JBUI.Borders.emptyTop(8)
    myErrorLabel.foreground = MessageType.ERROR.titleForeground

    val bottomPanel = JPanel(BorderLayout())
    bottomPanel.add(myErrorLabel, BorderLayout.SOUTH)
    bottomPanel.add(myAdvancedSettingsPlaceholder, BorderLayout.NORTH)

    add(myPanel, BorderLayout.NORTH)
    add(bottomPanel, BorderLayout.SOUTH)

    myLanguageComboBox.renderer = object : DefaultListCellRenderer() {
      override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean): Component {
        val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (component is JLabel && value is LanguageData) {
          component.text = value.language.displayName
          component.icon = value.icon
        }
        return component
      }
    }
    myLanguageComboBox.addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        onLanguageSelected((e.item as LanguageData).language)
      }
    }

    setupValidation()
    setDefaultValues()
    collectSupportedLanguages()
  }

  val course: Course
    get() {
      val course = Course()
      course.name = myTitleField.text
      course.description = myDescriptionTextArea.text
      course.setAuthorsAsString(StringUtil.splitByLines(myAuthorField.text.orEmpty()))
      course.language = mySelectedLanguage.id
      course.courseMode = CCUtils.COURSE_MODE
      return course
    }

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
    val message = when {
      myTitleField.text.isNullOrBlank() -> "Enter course title"
      myAuthorField.text.isNullOrBlank() -> "Enter course instructor"
      myDescriptionTextArea.text.isNullOrBlank() -> "Enter course description"
      locationString.isBlank() -> "Enter course location"
      !FileUtil.ensureCanCreateFile(File(FileUtil.toSystemDependentName(locationString))) -> "Can't create course at this location"
      else -> null
    }
    myErrorLabel.text = message
    myErrorLabel.isVisible = message != null
    myValidationListener?.onInputDataValidated(message == null)
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

  private fun onLanguageSelected(language: Language) {
    mySelectedLanguage = language

    val courseName = "${language.displayName.capitalize()} Course"
    val file = FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), courseName, "")
    if (!myTitleField.isChangedByUser) {
      myTitleField.setTextManually(file.name)
      if (!myPathField.isChangedByUser) {
        myPathField.setTextManually(file.absolutePath)
      }
    }

    val configurator = EduPluginConfigurator.INSTANCE.forLanguage(language) ?: return
    val labeledComponent = configurator.languageSettingsComponent(language)
    myAdvancedSettings.removeAll()
    myAdvancedSettings.add(myLocationField, BorderLayout.NORTH)
    if (labeledComponent != null) {
      myAdvancedSettings.add(labeledComponent, BorderLayout.SOUTH)
      UIUtil.mergeComponentsWithAnchor(myLocationField, labeledComponent)
    }
    myAdvancedSettings.revalidate()
    myAdvancedSettings.repaint()
  }

  private fun collectSupportedLanguages() {
    Extensions.getExtensions<LanguageExtensionPoint<EduPluginConfigurator>>(EduPluginConfigurator.EP_NAME, null)
            .mapNotNull { extension ->
              val languageId = extension.key
              val language = Language.findLanguageByID(languageId)
              if (language == null) {
                LOG.info("Language with id $languageId not found")
                null
              } else {
                val logo = extension.instance.eduCourseProjectGenerator?.directoryProjectGenerator?.logo
                LanguageData(language, logo)
              }
            }
            .sortedBy { (language, _) -> language.displayName }
            .forEach { myLanguageComboBox.addItem(it) }
  }

  private fun EduPluginConfigurator.languageSettingsComponent(language: Language): LabeledComponent<JComponent>? {
    // TODO: 'getLanguageSettingsComponent' should depend on language only
    val course = Course()
    course.language = language.id
    return eduCourseProjectGenerator?.getLanguageSettingsComponent(course)
  }

  companion object {
    private val LOG = Logger.getInstance(CCNewCoursePanel::class.java)
  }

  interface ValidationListener {
    fun onInputDataValidated(isInputDataComplete: Boolean)
  }

  private data class LanguageData(val language: Language, val icon: Icon?)

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
