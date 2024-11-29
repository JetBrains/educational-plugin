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
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.popup.AbstractPopup
import com.intellij.util.application
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.ai.messages.EduAIBundle
import com.jetbrains.edu.ai.translation.TranslationLoader
import com.jetbrains.edu.ai.translation.isSameLanguage
import com.jetbrains.edu.ai.translation.settings.AutoTranslationProperties
import com.jetbrains.edu.ai.translation.settings.translationSettings
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.ai.TranslationProjectSettings
import com.jetbrains.edu.learning.ai.TranslationProperties
import com.jetbrains.edu.learning.ai.translationSettings
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.educational.core.format.enum.TranslationLanguage
import java.awt.BorderLayout
import javax.swing.JComboBox
import javax.swing.JPanel
import javax.swing.JPanel.LEFT_ALIGNMENT

class CourseTranslationPopup(private val project: Project, private val course: EduCourse) {
  private var selectedLanguage: TranslationLanguage? =
    project.translationSettings().translationLanguage ?: application.translationSettings().preferableLanguage

  private lateinit var translateToCheckBox: Cell<JBCheckBox>
  private val comboBoxModel = TranslationLanguageComboBoxModel(course).apply { selectedItem = selectedLanguage }
  private lateinit var translationLanguageComboBox: JComboBox<TranslationLanguage>
  private val popup = createPopup()

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
  }

  private fun createCenterPanel(): DialogPanel = panel {
    row {
      translateToCheckBox = translateToCheckBox()
      translationLanguageComboBox = translationLanguageComboBox()
    }
    row {
      label(EduAIBundle.message("ai.translation.ai.powered.translation.to.multiple.languages")).applyToComponent {
        foreground = EduTranslationColors.aiTranslationBottomLabelTextColor
        border = JBEmptyBorder(2, 20, 8, 20)
      }
    }
    val translationProperties = project.translationSettings().translationProperties.value
    if (translationProperties != null) {
      feedbackFooter(translationProperties)
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
        enabled(selectedLanguage != null && !selectedLanguage.isSameLanguage(course))
        selected(TranslationProjectSettings.isCourseTranslated(project))
      }
      .onChanged(::translationCheckBoxChangeListener)

  private fun translationCheckBoxChangeListener(checkBox: JBCheckBox) {
    val language = selectedLanguage ?: return
    if (checkBox.isSelected && !language.isSameLanguage(course)) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, language)
    }
    else {
      project.translationSettings().setTranslation(null)
    }
    popup.closeOk(null)
  }

  private fun Row.translationLanguageComboBox(): JComboBox<TranslationLanguage> =
    comboBox(comboBoxModel)
      .bindItem(::selectedLanguage.toNullableProperty())
      .onChanged(::translationLanguageChangeListener)
      .focused()
      .align(Align.FILL)
      .component

  private fun translationLanguageChangeListener(component: ComboBox<TranslationLanguage>) {
    val language = component.selectedItem as? TranslationLanguage ?: return
    if (!language.isSameLanguage(course)) {
      TranslationLoader.getInstance(project).fetchAndApplyTranslation(course, language)
    }
    val settings = application.translationSettings()
    if (!settings.autoTranslate) {
      val autoTranslationProperties = AutoTranslationProperties(language, settings.autoTranslate)
      settings.setAutoTranslationProperties(autoTranslationProperties)
    }
    popup.closeOk(null)
  }

  private fun Panel.feedbackFooter(translationProperties: TranslationProperties): Row =
    row {
      link(EduAIBundle.message("ai.translation.share.your.feedback")) {
        val task = project.getCurrentTask() ?: return@link
        val dialog = AITranslationFeedbackDialog(project, course, task, translationProperties)
        this@CourseTranslationPopup.popup.closeOk(null)
        dialog.showAndGet()
      }.applyToComponent {
        background = EduTranslationColors.aiTranslationFooterLabelColor
      }
    }
}