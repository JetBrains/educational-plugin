package com.jetbrains.edu.ai.translation.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.JsonDataProvider
import com.intellij.platform.feedback.dialog.uiBlocks.TextDescriptionProvider
import com.intellij.ui.JBColor
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.put
import java.awt.Graphics
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Icon
import javax.swing.JButton

class LikeBlock(
  @NlsContexts.Label private val label: String,
  private val jsonElementName: String,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : FeedbackBlock, TextDescriptionProvider, JsonDataProvider {
  var answer: FeedbackLikenessAnswer = defaultLikeness
    private set
  private val likeOption = LikeOption().apply {
    isSelected = defaultLikeness == FeedbackLikenessAnswer.LIKE
  }
  private val dislikeOption = DislikeOption().apply {
    isSelected = defaultLikeness == FeedbackLikenessAnswer.DISLIKE
  }

  override fun addToPanel(panel: Panel) {
    panel.apply {
      row {
        label(label)
        cell(likeOption)
          .errorOnApply("") { answer == FeedbackLikenessAnswer.NO_ANSWER }
        cell(dislikeOption)
          .errorOnApply("") { answer == FeedbackLikenessAnswer.NO_ANSWER }
        bottomGap(BottomGap.SMALL)
      }
    }
  }

  override fun collectBlockTextDescription(stringBuilder: StringBuilder) {
    stringBuilder.apply {
      appendLine(label)
      appendLine(answer.result)
      appendLine()
    }
  }

  override fun collectBlockDataToJson(jsonObjectBuilder: JsonObjectBuilder) {
    jsonObjectBuilder.apply {
      put(jsonElementName, answer.result)
    }
  }

  private inner class LikeOption : FeedbackOption(AllIcons.Ide.Like, AllIcons.Ide.LikeSelected) {
    init {
      action = object : AbstractAction(null, unselectedIcon) {
        override fun actionPerformed(e: ActionEvent?) {
          answer = FeedbackLikenessAnswer.LIKE
          isSelected = true
          dislikeOption.isSelected = false
        }
      }
    }
  }

  private inner class DislikeOption : FeedbackOption(AllIcons.Ide.Dislike, AllIcons.Ide.DislikeSelected) {
    init {
      action = object : AbstractAction(null, unselectedIcon) {
        override fun actionPerformed(e: ActionEvent?) {
          answer = FeedbackLikenessAnswer.DISLIKE
          isSelected = true
          likeOption.isSelected = false
        }
      }
    }
  }

  private abstract inner class FeedbackOption(val unselectedIcon: Icon, private val selectedIcon: Icon) : JButton() {
    init {
      putClientProperty("styleTag", true)
      isFocusable = false
    }

    override fun setSelected(b: Boolean) {
      icon = if (b) selectedIcon else unselectedIcon
      if (getClientProperty("JComponent.outline") != null) {
        putClientProperty("JComponent.outline", null)
        repaint()
      }
      super.setSelected(b)
    }

    override fun paint(g: Graphics) {
      val backgroundColor = when {
        isSelected -> EduTranslationColors.aiTranslationFeedbackOptionSelectedBackgroundColor
        mousePosition != null -> EduTranslationColors.aiTranslationFeedbackOptionHoverBackgroundColor
        else -> JBColor.PanelBackground
      }
      putClientProperty("JButton.backgroundColor", backgroundColor)
      foreground = if (isSelected) {
        EduTranslationColors.aiTranslationFeedbackOptionSelectedForeground
      }
      else {
        EduTranslationColors.aiTranslationFeedbackOptionUnselectedForeground
      }
      super.paint(g)
    }
  }

  enum class FeedbackLikenessAnswer(val result: String) {
    NO_ANSWER("no answer"),
    LIKE("like"),
    DISLIKE("dislike")
  }
}