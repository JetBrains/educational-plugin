package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator

/**
 * Updates presentation of course nodes.
 *
 * Essentially, it does the same that [com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode.updateImpl] does but at different moment.
 * [com.intellij.ide.projectView.ProjectViewNodeDecorator.decorate] is called **after** `updateImpl` and can override all changes made before.
 * So, using this approach, we can be sure that other decorators don't override changes made in the presentation.
 */
class EduCourseViewNodeDecorator : ProjectViewNodeDecorator {
  override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
    val eduNode = node as? EduNode<*> ?: return

    data.clearText()
    data.locationString = null

    eduNode.updatePresentation(data)
  }
}
