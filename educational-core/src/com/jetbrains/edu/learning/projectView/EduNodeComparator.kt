package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AlphaComparator
import com.intellij.ide.util.treeView.NodeDescriptor
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.TaskFile
import java.util.*

object EduNodeComparator : Comparator<NodeDescriptor<*>> {
  override fun compare(o1: NodeDescriptor<*>?, o2: NodeDescriptor<*>?): Int {
    val parentNode1 = o1?.parentDescriptor
    val parentNode2 = o2?.parentDescriptor
    if (parentNode1 is TaskNode && parentNode1 == parentNode2) {
      val taskFile1 = o1.getTaskFile()
      val taskFile2 = o2.getTaskFile()
      if (taskFile1 != null && taskFile2 != null) {
        return Integer.compare(taskFile1.index, taskFile2.index)
      }
    }
    return AlphaComparator.INSTANCE.compare(o1, o2)
  }

  private fun NodeDescriptor<*>.getTaskFile(): TaskFile? {
    if (this !is PsiFileNode) {
      return null
    }
    return EduUtils.getTaskFile(project ?: return null, virtualFile ?: return null)
  }

  private val TaskFile.index: Int
    get() = task.taskFiles.values.indexOf(this)
}