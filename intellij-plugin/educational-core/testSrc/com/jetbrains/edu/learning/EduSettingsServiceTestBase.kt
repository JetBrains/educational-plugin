package com.jetbrains.edu.learning

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.intellij.lang.annotations.Language
import org.jdom.Element

abstract class EduSettingsServiceTestBase : EduTestCase() {

  protected inline fun <reified T : Any> PersistentStateComponent<T>.loadStateAndCheck(
    @Language("XML") xml: String,
    @Language("XML") expected: String = xml
  ) {
    val element = JDOMUtil.load(xml.trimIndent().toByteArray())
    val storedState = element.deserialize<T>()
    loadState(storedState)
    checkState<T>(expected)
  }

  protected inline fun <reified T : Any> PersistentStateComponent<T>.checkState(@Language("XML")  expected: String) {
    val currentState = state ?: error("Can't take state of `${javaClass.simpleName}`")
    val actual = JDOMUtil.writeElement(currentState.serialize())
    assertEquals(expected.trimIndent(), actual)
  }

  protected inline fun <reified T : Any> Element.deserialize(): T {
    if (Element::class.java.isAssignableFrom(T::class.java)) return this as T
    return XmlSerializer.deserialize(this, T::class.java)
  }

  protected inline fun <reified T : Any> T.serialize(): Element {
    if (this is Element) return this
    return XmlSerializer.serialize(this)
  }
}
