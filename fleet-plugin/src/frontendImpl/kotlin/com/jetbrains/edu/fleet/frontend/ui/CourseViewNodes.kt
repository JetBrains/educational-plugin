package com.jetbrains.edu.fleet.frontend.ui

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import fleet.frontend.icons.IconKeys
import noria.ui.components.*
import noria.ui.components.tree.TreeItem
import noria.ui.components.tree.treeCellRenderer
import noria.ui.text.uiText


fun courseNode(course: Course): TreeItem<Any> =
  TreeItem(key = course,
           item = course,
           expandable = true,
           autoExpand = true,
           children = { studyItems(course) }) { path, opts ->
    treeCellRenderer(path, opts, ::toolItemCellColors) { _, _, _ ->
      hbox(align = Align.Center) {
        icon(IconKeys.Folder)
        gap(width = 1)
        uiText(course.name)
      }
    }
  }

fun studyItems(itemContainer: ItemContainer): List<TreeItem<Any>> {
  return itemContainer.items.map { item ->
    when (item) {
      is ItemContainer -> containerNode(item)
      is Task -> taskNode(item)
      else -> eduItem(item)
    }
  }
}

fun taskItems(task: Task): List<TreeItem<TaskFile>> {
  return task.taskFiles.values.map {
    taskFileItem(it)
  }
}

private fun taskFileItem(it: TaskFile) = TreeItem(key = it,
                                                  item = it,
                                                  expandable = false,
                                                  autoExpand = false) { path, opts ->
  treeCellRenderer(path, opts, ::toolItemCellColors) { _, _, _ ->
    hbox(align = Align.Center) {
      gap(width = 1)
      uiText(it.name)
    }
  }
}

private fun eduItem(item: StudyItem) = TreeItem(key = item,
                                                item = item,
                                                expandable = true,
                                                autoExpand = false) { path, opts ->
  treeCellRenderer(path, opts, ::toolItemCellColors) { _, _, _ ->
    hbox(align = Align.Center) {
      gap(width = 1)
      uiText(item.name)
    }
  }
}

fun taskNode(item: Task): TreeItem<Any> =
  TreeItem(key = item,
           item = item,
           expandable = true,
           autoExpand = false,
           children = { taskItems(item) }) { path, opts ->
    treeCellRenderer(path, opts, ::toolItemCellColors) { _, _, _ ->
      hbox(align = Align.Center) {
        icon(IconKeys.Folder)
        gap(width = 1)
        uiText(item.name)
      }
    }
  }

fun containerNode(item: ItemContainer): TreeItem<Any> =
  TreeItem(key = item,
           item = item,
           expandable = true,
           autoExpand = false,
           children = { studyItems(item) }) { path, opts ->
    treeCellRenderer(path, opts, ::toolItemCellColors) { _, _, _ ->
      hbox(align = Align.Center) {
        icon(IconKeys.Folder)
        gap(width = 1)
        uiText(item.name)
      }
    }
  }
