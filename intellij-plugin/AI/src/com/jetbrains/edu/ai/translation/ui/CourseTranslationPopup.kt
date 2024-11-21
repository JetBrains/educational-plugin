package com.jetbrains.edu.ai.translation.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.JBPopup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.JBColor
import com.intellij.ui.TitlePanel
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.popup.AbstractPopup
import com.intellij.util.application
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.settings.translationSettings
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPanel.LEFT_ALIGNMENT

class CourseTranslationPopup(private val project: Project, private val course: EduCourse) {
  private val courseSourceLanguage = course.languageCode
  private var selectedLanguage: TranslationLanguage? =
    project.translationSettings().translationLanguage ?: application.translationSettings().preferableLanguage

  private lateinit var translateToCheckBox: Cell<JBCheckBox>
  private lateinit var translationLanguageComboBox: JComboBox<TranslationLanguage>
  private val popup: JBPopup = createPopup()

  fun show(point: RelativePoint) {
    popup.show(point)
  }

  private fun createPopup(): JBPopup {
    val panel = createPanel()
    val popup = JBPopupFactory.getInstance().createComponentPopupBuilder(panel, translationLanguageComboBox)
      .setTitle(EduAIBundle.getMessage("ai.translation.course.translation.dialog.title"))
      .createPopup()
    val titlePanel = (popup as AbstractPopup).title as TitlePanel
    titlePanel.label.apply {
      font = JBUI.Fonts.label().asBold()
      foreground = JBColor.BLACK
    }
    return popup
  }

  private fun createPanel(): JPanel = JPanel(BorderLayout()).apply {
    background = JBColor.WHITE
    border = JBEmptyBorder(0, 0, 4, 0)
    isOpaque = true

    add(createCenterPanel())
    add(createBottomLabel(), BorderLayout.SOUTH)
  }

  private fun createCenterPanel(): DialogPanel = panel {
    row {
      translateToCheckBox = translateToCheckBox()
      translationLanguageComboBox = translationLanguageComboBox()
    }
  }.apply {
    border = JBEmptyBorder(0, 20, 0, 20)
  }

  private fun Row.translateToCheckBox(): Cell<JBCheckBox> =
    checkBox(EduAIBundle.message("ai.translation.translate.to.label"))
      .applyToComponent {
        alignmentX = LEFT_ALIGNMENT
      }
      .apply {
        enabled(selectedLanguage?.isNotSource() == true)
        selected(TranslationProjectSettings.isCourseTranslated(project))
      }
      .onChanged(::translationCheckBoxChangeListener)

  private fun translationCheckBoxChangeListener(checkBox: JBCheckBox) {
    val language = selectedLanguage ?: return
    if (checkBox.isSelected && language.isNotSource()) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, language)
    }
    else if (!checkBox.isSelected || language.isSource()) {
      project.translationSettings().setTranslation(null)
    }
    popup.closeOk(null)
  }

  private fun Row.translationLanguageComboBox(): JComboBox<TranslationLanguage> =
    comboBox(LanguageComboBoxModel())
      .bindItem(::selectedLanguage.toNullableProperty())
      .onChanged(::translationLanguageChangeListener)
      .focused()
      .align(Align.FILL)
      .component

  private fun translationLanguageChangeListener(component: ComboBox<TranslationLanguage>) {
    val language = component.selectedItem as? TranslationLanguage ?: return
    if (language.isNotSource()) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, language)
    }
    else {
      project.translationSettings().setTranslation(null)
    }
    popup.closeOk(null)
  }

  private fun createBottomLabel(): JBLabel =
    JBLabel(EduAIBundle.message("ai.translation.ai.powered.translation.to.multiple.languages"))
      .apply {
        alignmentX = LEFT_ALIGNMENT
        foreground = EduColors.aiTranslationBottomLabelTextColor
        border = JBEmptyBorder(2, 20, 8, 20)
      }

  private fun TranslationLanguage.isSource(): Boolean = code == courseSourceLanguage
  private fun TranslationLanguage.isNotSource(): Boolean = !isSource()

  private inner class LanguageComboBoxModel : DefaultComboBoxModel<TranslationLanguage>() {
    init {
      @OptIn(ExperimentalStdlibApi::class)
      val languages = TranslationLanguage.entries
        .filter { it.isNotSource() }
        .sortedBy { it.label }
      addAll(languages)
    }

    override fun setSelectedItem(anObject: Any?) {
      val objectToSelect = anObject ?: EduAIBundle.message("ai.translation.choose.language")
      super.setSelectedItem(objectToSelect)
    }
  }
}