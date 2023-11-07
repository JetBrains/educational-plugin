package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ComponentUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursesList.CourseCardComponent
import java.awt.Component
import java.awt.event.*
import javax.swing.JComponent
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

class CourseGroupModel {
  private val courseCards: MutableList<CourseCardComponent> = mutableListOf()
  private var hoveredCard: CourseCardComponent? = null
  var selectedCard: CourseCardComponent? = null

  private val mouseListener: MouseAdapter = HoverMouseHandler()
  private val clickHandler: MouseAdapter = ClickMouseHandler()
  private val keyListener: KeyListener = SelectionKeyListener()

  var onSelection: () -> Unit = {}

  // BACKCOMPACT: 2023.1
  var onClick: (Course) -> Boolean = { false }

  fun addCourseCard(cardComponent: CourseCardComponent) {
    courseCards.add(cardComponent)
    cardComponent.getClickComponent().addMouseListener(mouseListener)
    addNavigationListenersRecursively(cardComponent)
    addClickListenerRecursively(cardComponent, cardComponent.actionComponent)
  }

  fun initialSelection() {
    if (courseCards.isNotEmpty()) {
      setSelection(courseCards[0])
    }
  }

  fun clear() {
    courseCards.clear()
    hoveredCard = null
    selectedCard = null
    onSelection()
  }

  fun setSelection(courseToSelect: Course?) {
    if (courseToSelect == null) return

    val component = courseCards.firstOrNull { it.course.course == courseToSelect }
    setSelection(component)
  }

  private fun setSelection(cardComponent: CourseCardComponent?) {
    clearSelection()
    if (hoveredCard === cardComponent) {
      hoveredCard = null
    }
    if (cardComponent != null && cardComponent != selectedCard) {
      selectedCard = cardComponent
      cardComponent.setSelection(isSelectedOrHover = true, scroll = true)
    }
    onSelection()
  }

  private fun clearSelection() {
    courseCards.forEach { it.setSelection(false) }
  }

  private fun getIndex(cardComponent: CourseCardComponent?): Int {
    if (cardComponent == null) return -1
    val index = courseCards.indexOf(cardComponent)
    assert(index >= 0) { cardComponent }
    return index
  }

  private fun addNavigationListeners(component: Component) {
    component.addMouseListener(mouseListener)
    component.addMouseMotionListener(mouseListener)
    component.addKeyListener(keyListener)
  }

  private fun addNavigationListenersRecursively(component: Component) {
    addNavigationListeners(component)
    for (child in UIUtil.uiChildren(component)) {
      addNavigationListenersRecursively(child)
    }
  }

  private fun addClickListenerRecursively(component: Component, nonClickableComponent: JComponent) {
    component.addMouseListener(clickHandler)
    for (child in UIUtil.uiChildren(component)) {
      if (child != nonClickableComponent) {
        addClickListenerRecursively(child, nonClickableComponent)
      }
    }
  }

  private fun getCourseCard(event: ComponentEvent): CourseCardComponent? {
    return ComponentUtil.getParentOfType(CourseCardComponent::class.java, event.component)
  }

  fun removeSelection() {
    selectedCard?.setSelection(false)
    selectedCard = null
  }


  private inner class ClickMouseHandler : MouseAdapter() {
    override fun mouseClicked(event: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(event)) {
        val cardComponent = getCourseCard(event) ?: return
        if (onClick(cardComponent.course)) {
          return
        }
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown {
          IdeFocusManager.getGlobalInstance().requestFocus(cardComponent as Component, true)
        }
        if (cardComponent == selectedCard) {
          return
        }
        setSelection(cardComponent)
      }
    }
  }

  private inner class HoverMouseHandler : MouseAdapter() {

    override fun mouseExited(event: MouseEvent) {
      hoveredCard?.setSelection(false)
      hoveredCard?.onHoverEnded()

      if (hoveredCard == null) {
        selectedCard?.onHoverEnded()
      }
      hoveredCard = null
    }

    override fun mouseMoved(event: MouseEvent) {
      val cardComponent = getCourseCard(event)
      if (cardComponent != selectedCard) {
        hoveredCard = cardComponent
      }

      cardComponent?.onHover(cardComponent == selectedCard)
    }
  }

  private inner class SelectionKeyListener : KeyAdapter() {
    override fun keyPressed(event: KeyEvent) {
      when (event.keyCode) {
        KeyEvent.VK_HOME, KeyEvent.VK_END -> {
          if (courseCards.isEmpty()) {
            return
          }
          event.consume()
          val index = if (event.keyCode == KeyEvent.VK_HOME) 0 else courseCards.size - 1
          setSelection(courseCards[index])
        }
        KeyEvent.VK_UP -> {
          event.consume()
          val selectionIndex = getIndex(selectedCard)
          val index = max(selectionIndex - 1, 0)
          setSelection(courseCards[index])
        }
        KeyEvent.VK_DOWN -> {
          event.consume()
          val selectionIndex = getIndex(selectedCard)
          val index = min(selectionIndex + 1, courseCards.size - 1)
          setSelection(courseCards[index])
        }
      }
    }
  }
}