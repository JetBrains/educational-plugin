package com.jetbrains.edu.learning.newproject.ui.coursePanel.groups

import com.intellij.ui.ComponentUtil
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.ui.coursePanel.CourseCardComponent
import java.awt.Component
import java.awt.event.*
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

class CourseGroupModel(private var selectionListener: () -> Unit) {
  private val courseCards: MutableList<CourseCardComponent> = mutableListOf()
  private var hoveredCard: CourseCardComponent? = null
  var selectedCard: CourseCardComponent? = null

  private val mouseHandler: MouseAdapter
  private val keyListener: KeyListener

  init {
    mouseHandler = object : MouseAdapter() {
      override fun mouseClicked(event: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(event)) {
          val cardComponent = getCourseCard(event)
          setSelection(cardComponent)
        }
      }

      override fun mouseExited(event: MouseEvent) {
        hoveredCard?.setSelection(false)
        hoveredCard = null
      }

      override fun mouseMoved(event: MouseEvent) {
        val cardComponent = getCourseCard(event)
        if (cardComponent != selectedCard) {
          hoveredCard = cardComponent
          cardComponent?.setSelection(true)
        }
      }
    }

    keyListener = object : KeyAdapter() {
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

  fun addCourseCard(cardComponent: CourseCardComponent) {
    courseCards.add(cardComponent)
    addListenersRecursively(cardComponent)
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
    selectionListener()
  }

  fun setSelection(courseToSelect: Course?) {
    if (courseToSelect == null) return

    val component = courseCards.firstOrNull { it.courseInfo.course == courseToSelect }
    setSelection(component)
  }

  private fun setSelection(cardComponent: CourseCardComponent?) {
    clearSelection()
    if (hoveredCard === cardComponent) {
      hoveredCard = null
    }
    if (cardComponent != null && cardComponent != selectedCard) {
      selectedCard = cardComponent
      cardComponent.setSelection(isSelectedOrHover = true, scrollAndFocus = true)
    }
    selectionListener()
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

  private fun addListeners(component: Component) {
    component.addMouseListener(mouseHandler)
    component.addMouseMotionListener(mouseHandler)
    component.addKeyListener(keyListener)
  }

  private fun addListenersRecursively(component: Component) {
    addListeners(component)
    for (child in UIUtil.uiChildren(component)) {
      addListenersRecursively(child)
    }
  }

  private fun getCourseCard(event: ComponentEvent): CourseCardComponent? {
    return ComponentUtil.getParentOfType(CourseCardComponent::class.java, event.component)
  }
}