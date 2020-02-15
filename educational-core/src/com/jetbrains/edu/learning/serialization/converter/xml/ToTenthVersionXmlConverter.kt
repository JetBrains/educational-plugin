package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.*
import org.jdom.Element

class ToTenthVersionXmlConverter : BaseXmlConverter() {

  override fun convertTaskElement(task: Element) {
    val additionalFileMap = getChildMap<String, String>(task, ADDITIONAL_FILES, true)
      .mapValues { (_, text) -> newAdditionalFileElement(text) }
    val additionalFilesElement = getChildWithName(task, ADDITIONAL_FILES, true)
    if (additionalFilesElement != null) {
      task.removeContent(additionalFilesElement)
    }
    addChildMap(task, ADDITIONAL_FILES, additionalFileMap)
  }

  private fun newAdditionalFileElement(text: String): Element {
    return Element(ADDITIONAL_FILE).apply {
      addChildWithName(this, TEXT, text)
      addChildWithName(this, VISIBLE, true)
    }
  }
}
