package com.jetbrains.edu.learning.codeforces

import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseFormat.Course
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants

class BrowseContestsPanel(courses: List<Course>) : JPanel(BorderLayout()) {
  private val NO_CONTESTS = "No contests found"

  val coursesList: JBList<Course> = JBList(courses)

  init {
    coursesList.setEmptyText(NO_CONTESTS)
    coursesList.cellRenderer = ContestListCellRenderer()

    if (coursesList.itemsCount > 0) {
      coursesList.selectedIndex = 0
    }

    val scrollPane = ScrollPaneFactory.createScrollPane(coursesList,
                                                        ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)

    scrollPane.horizontalScrollBar.isOpaque = true
    ScrollingUtil.installActions(coursesList)

    val listWithFilter = ListWithFilter.wrap(coursesList, scrollPane, Course::getName, true)

    add(listWithFilter, BorderLayout.CENTER)

    preferredSize = JBUI.size(600, 200)
  }

  private class ContestListCellRenderer : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(list: JList<*>?,
                                              value: Any?,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      val panel = JPanel(BorderLayout())
      panel.border = JBUI.Borders.empty(3)
      panel.background = UIUtil.getListBackground(isSelected, cellHasFocus)

      val contest = value as Course
      panel.add(JBLabel(contest.name), BorderLayout.CENTER)
      return panel
    }
  }
}