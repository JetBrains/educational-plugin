/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jetbrains.edu.learning.projectView

import com.intellij.ide.util.treeView.NodeRenderer
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.ui.CourseProgressBar
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeCellRenderer

internal class CourseTreeRenderer(private val myCourse: Course) : TreeCellRenderer {
  private var myCourseProgressBar: CourseProgressBar? = null
  private var myProgressCount: SimpleColoredComponent? = null

  override fun getTreeCellRendererComponent(tree: JTree, value: Any, selected: Boolean, expanded: Boolean,
                                            leaf: Boolean, row: Int, hasFocus: Boolean): Component {
    val nodeRenderer = NodeRenderer()
    val rendererComponent = nodeRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    if (value is DefaultMutableTreeNode) {
      val userObject = value.userObject
      if (userObject is CourseNode && myCourse.isStudy && !myCourse.isAdaptive) {
        val panel = JPanel(VerticalFlowLayout())
        panel.background = if (selected) UIUtil.getTreeSelectionBackground(hasFocus) else UIUtil.getTreeBackground()
        panel.foreground = UIUtil.getTreeForeground(selected, hasFocus)

        val nodePanel = JPanel(BorderLayout())
        nodePanel.background = if (selected) UIUtil.getTreeSelectionBackground(hasFocus) else UIUtil.getTreeBackground()
        nodePanel.add(rendererComponent, BorderLayout.WEST)
        myProgressCount = SimpleColoredComponent()
        nodePanel.add(myProgressCount!!, BorderLayout.EAST)
        val size = tree.parent.size
        nodePanel.preferredSize = Dimension(size.width - 45, nodePanel.preferredSize.height)

        myCourseProgressBar = CourseProgressBar(0.0, JBUI.scale(5), 5)
        updateCourseProgress(myCourse)

        panel.add(nodePanel)
        panel.add(myCourseProgressBar)
        return panel
      }
    }
    return rendererComponent
  }

  fun updateCourseProgress(course: Course) {
    val lessons = course.lessons

    var progress = ProgressUtil.countProgressAsOneTaskWithSubtasks(lessons)
    if (progress == null) {
      progress = ProgressUtil.countProgressWithoutSubtasks(lessons)
    }

    val taskSolved = progress.getFirst()
    val tasksTotal = progress.getSecond()
    val percent = taskSolved * 100.0 / tasksTotal

    myCourseProgressBar?.setFraction(percent / 100)
    myProgressCount?.clear()
    myProgressCount?.append(taskSolved.toString() + "/" + tasksTotal.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES)
  }
}
