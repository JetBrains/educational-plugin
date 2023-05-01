package com.jetbrains.edu.learning

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.intellij.lang.annotations.Language

abstract class EduSettingsServiceTestBase : EduTestCase() {

  protected inline fun <reified T> PersistentStateComponent<T>.loadStateAndCheck(@Language("XML") xml: String) {
    val element = JDOMUtil.load(xml.trimIndent().toByteArray())
    val storedState = XmlSerializer.deserialize(element, T::class.java)
    loadState(storedState)
    val currentState = state ?: error("Can't take state of `${javaClass.simpleName}`")
    val actual = JDOMUtil.writeElement(XmlSerializer.serialize(currentState))
    assertEquals(xml.trimIndent(), actual)
  }
}
