package com.jetbrains.edu.learning.taskDescription.ui.uihtml.steps

import com.jetbrains.edu.learning.courseFormat.tasks.VideoTask
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.stepik.getStepikLink
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.UIModeIndependentHtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext

object VideoTaskFilter : UIModeIndependentHtmlTransformer() {
  override fun transform(html: String, context: HtmlTransformerContext): String {
    val task = context.task as? VideoTask ?: return html
    return EduCoreBundle.message("stepik.view.video", getStepikLink(task, task.lesson))
  }
}