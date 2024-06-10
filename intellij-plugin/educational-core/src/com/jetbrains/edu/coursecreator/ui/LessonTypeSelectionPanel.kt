package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsContexts
import com.intellij.ui.JBColor
import com.intellij.ui.RoundedLineBorder
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.UnscaledGaps
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.ui.errors.ErrorComponent
import com.jetbrains.edu.learning.newproject.ui.errors.ValidationMessage
import com.jetbrains.edu.learning.taskToolWindow.ui.addBorder
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.edu.learning.ui.RoundedWrapper
import java.awt.event.*
import javax.swing.JComponent
import javax.swing.JLabel

class LessonTypeSelectionPanel(parentDisposable: Disposable, coursePanel: CCNewCoursePanel) : Wrapper() {
  private val errorComponent = ErrorComponent {}.apply {
    setErrorMessage(ValidationMessage(EduCoreBundle.message("cc.new.course.lesson.selection.empty.error")))
  }

  private val lessonTypePanel = LessonChoicePanel {
    errorComponent.isVisible = false
  }

  val isFrameworkLessonSelected: Boolean?
    get() = lessonTypePanel.isFrameworkLessonCardSelected

  init {
    val panel = panel {
      row {
        text(EduCoreBundle.message("cc.new.course.lesson.selection.title")).applyToComponent {
          font = JBFont.h3()
        }.customize(UnscaledGaps(bottom = 0))
      }
      row {
        text(EduCoreBundle.message("cc.new.course.lesson.selection.hint")).applyToComponent {
          foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        }
      }.bottomGap(BottomGap.SMALL)

      row {
        cell(lessonTypePanel)
          .align(AlignY.FILL)
          .cellValidation {
            addApplyRule {
              if (lessonTypePanel.isFrameworkLessonCardSelected == null) {
                errorComponent.apply {
                  isVisible = true
                }
                ValidationInfo(EduCoreBundle.message("cc.new.course.lesson.selection.empty.error"))
              }
              else null
            }
          }
        resizableRow()
      }.bottomGap(BottomGap.SMALL)

      row {
        cell(errorComponent).align(AlignX.FILL)
      }.bottomGap(BottomGap.SMALL)

      val feedbackPanel = coursePanel.createFeedbackPanel()
      row {
        cell(feedbackPanel)
          .align(AlignX.RIGHT)
          .align(AlignY.BOTTOM)
      }.bottomGap(BottomGap.SMALL)
      separator()
      align(Align.FILL)
    }.apply {
      border = JBUI.Borders.empty(8, 8, 0, 4)
    }
    panel.registerValidators(parentDisposable)
    setContent(panel)
    preferredSize = JBUI.size(600, preferredSize.height)
    minimumSize = JBUI.size(600, minimumSize.height)
  }

  fun validateAll(): List<ValidationInfo> = (targetComponent as DialogPanel).validateAll()
}

class LessonChoicePanel(private val onSelection: () -> Unit) : Wrapper(), Disposable {
  private val lessonCardSimpleLesson = LessonCard(
    EduCoreBundle.message("cc.new.course.lesson.selection.card.simple.title"),
    EduCoreBundle.message("cc.new.course.lesson.selection.card.simple.description"),
    false
  )
  private val lessonCardFrameworkLesson = LessonCard(
    EduCoreBundle.message("cc.new.course.lesson.selection.card.framework.title"),
    EduCoreBundle.message("cc.new.course.lesson.selection.card.framework.description"),
    true
  )

  /**
   * false -> Simple lesson card selected
   * true -> Framework lesson card selected
   * null -> nothing is selected
   */
  var isFrameworkLessonCardSelected: Boolean? = null
    private set(value) {
      if (field != value) {
        lessonCardSimpleLesson.update(value == false)
        lessonCardFrameworkLesson.update(value == true)
        onSelection()
        revalidate()
        repaint()
      }
      field = value
    }

  private val selectedCard: LessonCard?
    get() = when (isFrameworkLessonCardSelected) {
      false -> lessonCardSimpleLesson
      true -> lessonCardFrameworkLesson
      null -> null
    }

  init {
    val panel = panel {
      row {
        cell(RoundedWrapper(lessonCardSimpleLesson))
          .widthGroup("LessonCard")
          .align(AlignY.FILL)
          .resizableColumn()
        cell(RoundedWrapper(lessonCardFrameworkLesson))
          .widthGroup("LessonCard")
          .align(AlignY.FILL)
          .resizableColumn()
      }.layout(RowLayout.PARENT_GRID)
      align(Align.FILL)
    }.apply {
      isFocusable = true
      addFocusListener(object : FocusAdapter() {
        override fun focusGained(e: FocusEvent?) {
          super.focusGained(e)
          selectedCard?.update(true)
        }

        override fun focusLost(e: FocusEvent?) {
          super.focusLost(e)
          selectedCard?.update(isSelected = true, isFocused = false)
        }
      })
    }
    val actionLeft = DumbAwareAction.create { isFrameworkLessonCardSelected = false }
    actionLeft.registerCustomShortcutSet(ActionUtil.getShortcutSet("LessonCard-left"), this)
    val actionRight = DumbAwareAction.create { isFrameworkLessonCardSelected = true }
    actionRight.registerCustomShortcutSet(ActionUtil.getShortcutSet("LessonCard-right"), this)

    setContent(panel)
  }

  private fun createLessonCardMouseListener(lessonCard: LessonCard): MouseListener {
    return object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        super.mousePressed(e)
        requestFocusInWindow()
        isFrameworkLessonCardSelected = lessonCard.isFrameworkLessonCard
      }
    }
  }

  private inner class LessonCard(
    @NlsContexts.Label title: String,
    @NlsContexts.Label description: String,
    val isFrameworkLessonCard: Boolean
  ) : Wrapper() {
    private lateinit var titleComponent: JComponent
    private lateinit var iconComponent: JLabel

    init {
      isOpaque = true
      val panel = panel {
        row {
          iconComponent = icon(EducationalCoreIcons.LessonCardSimpleLesson).component
        }.bottomGap(BottomGap.SMALL)

        row {
          titleComponent = text(title)
            .customize(UnscaledGaps(bottom = 0))
            .component.apply {
              foreground = EduColors.lessonCardForeground
              // add a mouselistener because otherwise it does not track clicks on the text field
              addMouseListener(createLessonCardMouseListener(this@LessonCard))
            }
        }
        row {
          text(description).applyToComponent {
            font = JBFont.medium()
            foreground = EduColors.lessonCardSecondaryForeground
            addMouseListener(createLessonCardMouseListener(this@LessonCard))
          }
        }
      }.apply {
        isOpaque = false
        addMouseListener(createLessonCardMouseListener(this@LessonCard))
      }
      setContent(panel)
      update(false)
    }
    
    fun update(isSelected: Boolean, isFocused: Boolean = isSelected) {
      border = innerBorder

      if (isSelected) {
        titleComponent.font = selectedTitleFont
        background = EduColors.lessonCardSelectedBackground
        iconComponent.icon = if (isFrameworkLessonCard) {
          EducationalCoreIcons.LessonCardGuidedProjectSelected
        }
        else {
          EducationalCoreIcons.LessonCardSimpleLessonSelected
        }
        addBorder(if(isFocused) focusedBorder else selectedBorder)
      } else {
        titleComponent.font = defaultTitleFont
        background = EduColors.lessonCardBackground
        iconComponent.icon = if (isFrameworkLessonCard) {
          EducationalCoreIcons.LessonCardGuidedProject
        }
        else {
          EducationalCoreIcons.LessonCardSimpleLesson
        }
        addBorder(defaultBorder)
      }
    }
  }

  companion object {
    private const val ARC_SIZE = 8
    private const val ROUNDED_BORDER_SIZE = 1
    private const val FOCUSED_ROUNDED_BORDER_SIZE = 2

    private val innerBorder = JBUI.Borders.empty(8, 12, 8, 6)
    private val outerBorder = RoundedLineBorder(JBColor.background(), ARC_SIZE, FOCUSED_ROUNDED_BORDER_SIZE - ROUNDED_BORDER_SIZE)
    private val defaultBorder = JBUI.Borders.compound(
      RoundedLineBorder(EduColors.lessonCardBorderColor, ARC_SIZE, ROUNDED_BORDER_SIZE),
      outerBorder
    )
    private val selectedBorder = JBUI.Borders.compound(
      RoundedLineBorder(EduColors.lessonCardSelectedBorderColor, ARC_SIZE, ROUNDED_BORDER_SIZE),
      outerBorder
    )
    private val focusedBorder = RoundedLineBorder(EduColors.lessonCardFocusedBorderColor, ARC_SIZE, FOCUSED_ROUNDED_BORDER_SIZE)

    private val selectedTitleFont = JBFont.regular().asBold()
    private val defaultTitleFont = JBFont.regular()
  }

  override fun dispose() {}
}
