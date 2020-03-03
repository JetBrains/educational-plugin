package com.jetbrains.edu.learning.newproject.ui.coursePanel

import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.AbstractLayoutManager
import com.intellij.util.ui.JBValue
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel

/**
 * Inspired by [com.intellij.ide.plugins.newui.BaselinePanel]
 */
class BaselinePanel : NonOpaquePanel() {
  private val myButtonComponents: MutableList<Component> = ArrayList()

  init {
    layout = BaselineLayout(myButtonComponents)
  }

  override fun add(component: Component): Component {
    (layout as BaselineLayout).baseComponent = component
    return super.add(component)
  }

  fun setYOffset(yOffset: Int) {
    (layout as BaselineLayout).yOffset = yOffset
  }

  fun addButtonComponent(component: JComponent) {
    myButtonComponents.add(component)
    add(component, null)
  }
}

private class BaselineLayout(
  private val myButtonComponents: MutableList<Component> = ArrayList()
) : AbstractLayoutManager() {
  private val myOffset: JBValue = JBValue.Float(8f)
  private val myBeforeButtonOffset: JBValue = JBValue.Float(40f)
  private val myButtonOffset: JBValue = JBValue.Float(6f)
  var yOffset = 0
  lateinit var baseComponent: Component

  override fun preferredLayoutSize(parent: Container): Dimension {
    val baseSize = baseComponent.preferredSize
    var width = baseSize.width
    val size = myButtonComponents.size
    if (size > 0) {
      var visibleCount = 0
      for (component in myButtonComponents) {
        if (component.isVisible) {
          width += component.preferredSize.width
          visibleCount++
        }
      }
      if (visibleCount > 0) {
        width += myBeforeButtonOffset.get()
        width += (visibleCount - 1) * myButtonOffset.get()
      }
    }
    val insets = parent.insets
    return Dimension(width, insets.top + baseSize.height + insets.bottom)
  }

  private fun calculateBaseWidth(parent: Container): Int {
    var parentWidth = parent.width
    var visibleCount = 0
    for (component in myButtonComponents) {
      if (component.isVisible) {
        parentWidth -= component.preferredSize.width
        visibleCount++
      }
    }
    parentWidth -= myButtonOffset.get() * (visibleCount - 1)
    if (visibleCount > 0) {
      parentWidth -= myOffset.get()
    }
    return parentWidth
  }

  override fun layoutContainer(parent: Container) {
    val baseSize = baseComponent.preferredSize
    val top = parent.insets.top
    val y = top + baseComponent.getBaseline(baseSize.width, baseSize.height)
    val x = 0
    val calcBaseWidth = calculateBaseWidth(parent)
    if (baseComponent is JLabel) {
      val label = baseComponent as JLabel
      label.toolTipText = if (calcBaseWidth < baseSize.width) label.text else null
    }
    baseSize.width = baseSize.width.coerceAtMost(calcBaseWidth)
    baseComponent.setBounds(x, top, baseSize.width, baseSize.height)
    var lastX = parent.width
    for (i in myButtonComponents.indices.reversed()) {
      val component = myButtonComponents[i]
      if (!component.isVisible) {
        continue
      }
      val size = component.preferredSize
      lastX -= size.width
      setBaselineBounds(lastX, y - yOffset, component, size)
      lastX -= myButtonOffset.get()
    }
  }

  private fun setBaselineBounds(x: Int, y: Int, component: Component, size: Dimension) {
    setBaselineBounds(x, y, component, size, size.width, size.height)
  }

  private fun setBaselineBounds(x: Int, y: Int, component: Component, prefSize: Dimension, width: Int, height: Int) {
    component.setBounds(x, y - component.getBaseline(prefSize.width, prefSize.height), width, height)
  }
}