package com.jetbrains.edu.learning.twitter.ui

import com.intellij.openapi.Disposable
import java.nio.file.Path
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JLabel

fun createImageComponent(path: Path, @Suppress("UNUSED_PARAMETER") disposable: Disposable): JComponent {
  return JLabel(ImageIcon(path.toString()))
}
