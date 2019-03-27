package com.jetbrains.edu.learning.serialization.converter.xml

import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.getChildWithName
import com.jetbrains.edu.learning.serialization.SerializationUtils.Xml.renameElement
import org.jdom.Element

class To14VersionXmlConverter : BaseXmlConverter() {

  override fun convertTaskElement(task: Element) {
    val stepId = getChildWithName(task, "stepId")
    renameElement(stepId, "id")
  }
}