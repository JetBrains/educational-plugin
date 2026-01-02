package com.jetbrains.edu.uiOnboarding.steps

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.toolbarLayout.ToolbarLayoutStrategy
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.ui.ColorUtil
import com.intellij.ui.DrawUtil
import com.intellij.ui.GotItComponentBuilder.Companion.getArrowShift
import com.intellij.ui.JBColor
import com.intellij.ui.RemoteTransferUIManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimation
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.EYE_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationData.Companion.SMALL_SHIFT
import com.jetbrains.edu.uiOnboarding.EduUiOnboardingAnimationStep
import com.jetbrains.edu.uiOnboarding.ZhabaComponent
import com.jetbrains.edu.uiOnboarding.actions.ZHABA_SAYS_ACTION_PLACE
import com.jetbrains.edu.uiOnboarding.stepsGraph.ActionGroupZhabaData
import com.jetbrains.edu.uiOnboarding.stepsGraph.GraphData
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep
import com.jetbrains.edu.uiOnboarding.stepsGraph.ZhabaStep.Companion.FINISH_TRANSITION
import kotlinx.coroutines.CoroutineScope
import java.awt.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder


/**
 * This step is displayed as a ballon with a
 * - [title]
 * - vertically layered buttons for actions of the [actionGroup]
 *
 * This step generates transitions only if it is interrupted.
 * The idea is that it will be interrupted by calling some action from the [actionGroup].
 */
class ActionGroupZhabaStep(
  override val stepId: String,
  private val actionGroup: ActionGroup,
  @NlsContexts.NotificationTitle private val title: String
) : ZhabaStep<ActionGroupZhabaData, GraphData.EMPTY> {

  private fun buildAnimation(data: EduUiOnboardingAnimationData, point: RelativePoint): EduUiOnboardingAnimation =
    object : EduUiOnboardingAnimation {
      override val steps: List<EduUiOnboardingAnimationStep> = listOfNotNull(
        EduUiOnboardingAnimationStep(data.lookRight, point, point, 2_000),
        EduUiOnboardingAnimationStep(data.lookLeft, point, point, 1_000),
      )

      override val cycle: Boolean = true
    }

  private val useContrastColors = false

  override fun performStep(
    project: Project,
    data: EduUiOnboardingAnimationData
  ): ActionGroupZhabaData? {
    val relativeZhabaPoint = locateZhabaInProjectToolWindow(project) ?: return null
    val zhabaPoint = relativeZhabaPoint.originalPoint
    val component = relativeZhabaPoint.originalComponent

    val zhabaComponent = ZhabaComponent(project)
    zhabaComponent.animation = buildAnimation(data, relativeZhabaPoint)

    // Position the balloon at the bottom of the project view component
    val tooltipPoint = Point(zhabaPoint.x + EYE_SHIFT, zhabaPoint.y - SMALL_SHIFT)
    val tooltipRelativePoint = RelativePoint(component, tooltipPoint)

    // Copied and simplified from com.intellij.ui.GotItComponentBuilder.createContent.
    val builder = JBPopupFactory.getInstance()
      .createBalloonBuilder(createContent())
      .setHideOnAction(false)
      .setHideOnClickOutside(false)
      .setHideOnFrameResize(false)
      .setHideOnKeyOutside(false)
      .setHideOnClickOutside(false)
      .setBlockClicksThroughBalloon(true)
      .setRequestFocus(false)
      .setBorderColor(getBorderColor())
      .setCornerToPointerDistance(getArrowShift())
      .setFillColor(JBUI.CurrentTheme.GotItTooltip.background(false))
      .setPointerSize(JBUI.size(16, 8))
      .setCornerRadius(JBUI.CurrentTheme.GotItTooltip.CORNER_RADIUS.get())

    return ActionGroupZhabaData(builder, tooltipRelativePoint, Balloon.Position.above, relativeZhabaPoint, zhabaComponent)
  }

  override suspend fun executeStep(
    stepData: ActionGroupZhabaData,
    graphData: GraphData.EMPTY,
    cs: CoroutineScope,
    disposable: Disposable
  ): String {
    val builder = stepData.builder
    builder.setCloseButtonEnabled(false)
    builder.setDisposable(disposable)

    val showInCenter = stepData.position == null
    val balloon = builder.createBalloon()
    balloon.setAnimationEnabled(false)

    if (showInCenter) {
      balloon.showInCenterOf(stepData.tooltipPoint.originalComponent as JComponent)
    }
    else {
      balloon.show(stepData.tooltipPoint, stepData.position)
    }

    return stepData.zhaba.start(cs) ?: FINISH_TRANSITION
  }

  private fun getBorderColor(): Color {
    val borderColor = JBUI.CurrentTheme.GotItTooltip.borderColor(useContrastColors)
    val simpleBorderColor = JBColor.namedColor("GotItTooltip.borderSimplifiedColor", borderColor)
    return JBColor.lazy { if (DrawUtil.isSimplifiedUI()) simpleBorderColor else borderColor }
  }

  private fun createContent(): JComponent {
    val actionToolbar = createActionToolbar()

    // Everything that follows is copied and simplified from com.intellij.ui.GotItComponentBuilder.createContent.
    // We suppose that there is no icon and no step index in the ballon, also no buttons at the bottom.
    // The message text is substituted with the action toolbar.
    val panel = JPanel(GridBagLayout())
    val gc = GridBag()
    val left = 0
    val column = 0

    if (title.isNotEmpty()) {
      gc.nextLine()

      val finalText = HtmlChunk.raw(title)
        .bold()
        .wrapWith(HtmlChunk.font(ColorUtil.toHtmlColor(JBUI.CurrentTheme.GotItTooltip.headerForeground(useContrastColors))))
        .wrapWith(HtmlChunk.html())
        .toString()
      val constraints = gc.setColumn(column).anchor(GridBagConstraints.LINE_START).insets(1, left, 0, 0)
      panel.add(JBLabel(finalText), constraints)
    }

    gc.nextLine()

    val constraints = gc
      .setColumn(column)
      .anchor(GridBagConstraints.LINE_START)
      .insets(if (title.isNotEmpty()) JBUI.CurrentTheme.GotItTooltip.TEXT_INSET.get() else 0, left, 0, 0)
    panel.add(actionToolbar.component, constraints)

    panel.background = JBUI.CurrentTheme.GotItTooltip.background(useContrastColors)
    panel.border = EmptyBorder(JBUI.CurrentTheme.GotItTooltip.insets())

    RemoteTransferUIManager.forceDirectTransfer(panel)
    return panel
  }

  private fun createActionToolbar(): ActionToolbar {
    val actionToolbar = ActionManager.getInstance().createActionToolbar(ZHABA_SAYS_ACTION_PLACE, actionGroup, false)

    // We need gaps between buttons. It is possible to set up the Toolbar if the components for actions are ActionButton, but
    // we use plain JButtons, so we need to add gaps some other way.
    // Here we delegate the layout to the existing layout strategy but modify its layout adding gaps over each button.
    actionToolbar.layoutStrategy = addGapsInLayoutStrategy(
      actionToolbar.layoutStrategy,
      JBUI.scale(16)
    )

    actionToolbar.setMiniMode(false)
    actionToolbar.component.background = JBUI.CurrentTheme.GotItTooltip.background(useContrastColors)
    actionToolbar.component.border = JBUI.Borders.empty()
    return actionToolbar
  }

  private fun addGapsInLayoutStrategy(
    existingLayoutStrategy: ToolbarLayoutStrategy,
    gap: Int
  ): ToolbarLayoutStrategy = object : ToolbarLayoutStrategy {
    override fun calculateBounds(toolbar: ActionToolbar): List<Rectangle?>? {
      val result = existingLayoutStrategy.calculateBounds(toolbar) ?: return null
      return result.mapIndexed { index, rectangle ->
        Rectangle(rectangle.x, rectangle.y + (index + 1) * gap, rectangle.width, rectangle.height)
      }
    }

    override fun calcPreferredSize(toolbar: ActionToolbar): Dimension? {
      val actions = toolbar.actions.size
      val result = existingLayoutStrategy.calcPreferredSize(toolbar) ?: return null
      return Dimension(result.width, result.height + actions * gap)
    }

    override fun calcMinimumSize(toolbar: ActionToolbar): Dimension? {
      val actions = toolbar.actions.size
      val result = existingLayoutStrategy.calcPreferredSize(toolbar) ?: return null
      return Dimension(result.width, result.height + actions * gap)
    }
  }
}