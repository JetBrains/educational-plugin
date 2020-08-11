package com.jetbrains.edu.learning.twitter.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.jcef.JBCefBrowser
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefContextMenuParams
import org.cef.callback.CefMenuModel
import java.awt.Dimension
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.JComponent

fun createImageComponent(path: Path, disposable: Disposable): JComponent {
  val browser = TwitterJBCefBrowser(path)
  Disposer.register(disposable, browser)
  val component = browser.component
  component.preferredSize = calculateImageDimension(path)
  return component
}

private fun calculateImageDimension(path: Path): Dimension {
  // TODO: it can be done more efficiently
  val icon = ImageIcon(path.toString())
  return Dimension(icon.iconWidth, icon.iconHeight)
}

private class TwitterJBCefBrowser(path: Path) : JBCefBrowser(path.toUri().toString()) {
  override fun createDefaultContextMenuHandler(): DefaultCefContextMenuHandler {
    val isInternal = ApplicationManager.getApplication().isInternal
    return object : DefaultCefContextMenuHandler(isInternal) {
      override fun onBeforeContextMenu(browser: CefBrowser, frame: CefFrame, params: CefContextMenuParams, model: CefMenuModel) {
        // Clear context menu
        // Inspired by `com.intellij.ui.jcef.JCEFHtmlPanel`
        model.clear()
        super.onBeforeContextMenu(browser, frame, params, model)
      }
    }
  }
}
