import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformer
import com.jetbrains.edu.learning.taskDescription.ui.uihtml.HtmlTransformerContext
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintJCEF
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintSwing
import com.jetbrains.edu.learning.taskDescription.ui.wrapHintTagsInsideHTML

object HintsWrapper : HtmlTransformer {
  override fun swingTransform(html: String, context: HtmlTransformerContext): String = wrapHintTagsInsideHTML(html) { e, number, title ->
    wrapHintSwing(context.project, e, number, title)
  }

  override fun jcefTransform(html: String, context: HtmlTransformerContext): String = wrapHintTagsInsideHTML(html) { e, number, title ->
    wrapHintJCEF(context.project, e, number, title)
  }
}