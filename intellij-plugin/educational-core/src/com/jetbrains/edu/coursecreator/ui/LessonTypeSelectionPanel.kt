package com.jetbrains.edu.coursecreator.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.DumbAwareAction
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
import com.jetbrains.edu.learning.taskToolWindow.ui.addBorder
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.edu.learning.ui.RoundedWrapper
import java.awt.event.*
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class LessonTypeSelectionPanel(coursePanel: CCNewCoursePanel) : Wrapper() {
  private val lessonTypePanel = LessonChoicePanel()

  val isGuidedProjectSelected: Boolean
    get() = lessonTypePanel.isGuidedProjectCardSelected

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
        resizableRow()
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
    setContent(panel)
    preferredSize = JBUI.size(600, preferredSize.height)
    minimumSize = JBUI.size(600, minimumSize.height)
  }
}

class LessonChoicePanel : Wrapper(), Disposable {
  private val lessonCardSimpleLesson = SimpleLessonCard()
  private val lessonCardGuidedProject = GuidedProjectCard()

  /**
   * false -> Simple lesson card selected
   * true -> Guided Project card selected
   */
  val isGuidedProjectCardSelected: Boolean
    get() = when(selectedCard) {
      is SimpleLessonCard -> false
      is GuidedProjectCard -> true
    }

  private var selectedCard: LessonCard = lessonCardSimpleLesson
    set(value) {
      if (field != value) {
        field.update(false)
        value.update(true)
        revalidate()
        repaint()
      }
      field = value
    }

  init {
    val panel = panel {
      row {
        cell(RoundedWrapper(lessonCardSimpleLesson))
          .widthGroup("LessonCard")
          .align(AlignY.FILL)
          .resizableColumn()
        cell(RoundedWrapper(lessonCardGuidedProject))
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
          selectedCard.update(true)
        }

        override fun focusLost(e: FocusEvent?) {
          super.focusLost(e)
          selectedCard.update(isSelected = true, isFocused = false)
        }
      })
    }
    val actionLeft = DumbAwareAction.create { selectedCard = lessonCardSimpleLesson }
    actionLeft.registerCustomShortcutSet(ActionUtil.getShortcutSet("LessonCard-left"), this)
    val actionRight = DumbAwareAction.create { selectedCard = lessonCardGuidedProject }
    actionRight.registerCustomShortcutSet(ActionUtil.getShortcutSet("LessonCard-right"), this)

    lessonCardSimpleLesson.addSelectionListener { selectedCard = lessonCardSimpleLesson }
    lessonCardGuidedProject.addSelectionListener { selectedCard = lessonCardGuidedProject }

    setContent(panel)
  }

  override fun dispose() {}
}

private sealed class LessonCard() : Wrapper() {
  protected abstract val icon: Icon
  protected abstract val selectedIcon: Icon
  protected abstract val title: @NlsContexts.Label String
  protected abstract val description: @NlsContexts.Label String

  private lateinit var titleComponent: JComponent
  private lateinit var iconComponent: JLabel

  private val selectionListeners: MutableList<CardSelectionListener> = mutableListOf()

  init {
    isOpaque = true
    val panel = panel {
      row {
        iconComponent = icon(icon).component
      }.bottomGap(BottomGap.SMALL)

      row {
        titleComponent = text(title)
          .customize(UnscaledGaps(bottom = 0))
          .component.apply {
            foreground = EduColors.lessonCardForeground
            // add a mouselistener because otherwise it does not track clicks on the text field
            addMouseListener(createLessonCardMouseListener())
          }
      }
      row {
        text(description).applyToComponent {
          font = JBFont.medium()
          foreground = EduColors.lessonCardSecondaryForeground
          addMouseListener(createLessonCardMouseListener())
        }
      }
    }.apply {
      isOpaque = false
      addMouseListener(createLessonCardMouseListener())
    }
    setContent(panel)
    update(false)
  }

  fun update(isSelected: Boolean, isFocused: Boolean = isSelected) {
    border = innerBorder

    if (isSelected) {
      titleComponent.font = selectedTitleFont
      background = EduColors.lessonCardSelectedBackground
      iconComponent.icon = selectedIcon
      addBorder(if(isFocused) focusedBorder else selectedBorder)
    } else {
      titleComponent.font = defaultTitleFont
      background = EduColors.lessonCardBackground
      iconComponent.icon = icon
      addBorder(defaultBorder)
    }
  }

  fun addSelectionListener(onSelected: () -> Unit) {
    selectionListeners.add(object : CardSelectionListener {
      override fun onSelected() {
        onSelected()
      }
    })
  }

  private fun createLessonCardMouseListener(): MouseListener {
    return object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent?) {
        super.mousePressed(e)
        requestFocusInWindow()
        selectionListeners.forEach(CardSelectionListener::onSelected)
      }
    }
  }

  private interface CardSelectionListener {
    fun onSelected()
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
}

private class SimpleLessonCard() : LessonCard() {
  override val icon: Icon
    get() = EducationalCoreIcons.LessonCardSimpleLesson
  override val selectedIcon: Icon
    get() = EducationalCoreIcons.LessonCardSimpleLessonSelected
  override val title: String
    get() = EduCoreBundle.message("cc.new.course.lesson.selection.card.simple.title")
  override val description: String
    get() = EduCoreBundle.message("cc.new.course.lesson.selection.card.simple.description")
}

private class GuidedProjectCard() : LessonCard() {
  override val icon: Icon
    get() = EducationalCoreIcons.LessonCardGuidedProject
  override val selectedIcon: Icon
    get() = EducationalCoreIcons.LessonCardGuidedProjectSelected
  override val title: String
    get() = EduCoreBundle.message("cc.new.course.lesson.selection.card.framework.title")
  override val description: String
    get() = EduCoreBundle.message("cc.new.course.lesson.selection.card.framework.description")
}