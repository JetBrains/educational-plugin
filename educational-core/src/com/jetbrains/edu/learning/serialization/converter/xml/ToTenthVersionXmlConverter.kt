package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class ToTenthVersionXmlConverter : BaseXmlConverter() {

  override fun convertTaskElement(task: Element) {
    val additionalFileMap = getChildMap<String, String>(task, ADDITIONAL_FILES)
      .mapValues { (_, text) -> newAdditionalFileElement(text) }
    task.removeContent(getChildWithName(task, ADDITIONAL_FILES))
    addChildMap(task, ADDITIONAL_FILES, additionalFileMap)
  }

  private fun newAdditionalFileElement(text: String): Element {
    return Element(ADDITIONAL_FILE).apply {
      addChildWithName(this, TEXT, text)
      addChildWithName(this, VISIBLE, true)
    }
  }
}
