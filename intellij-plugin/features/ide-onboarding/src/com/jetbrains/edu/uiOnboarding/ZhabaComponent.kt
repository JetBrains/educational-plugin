package com.jetbrains.edu.uiOnboarding

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.util.ImageLoader
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.Image
import javax.swing.JComponent


class ZhabaComponent(val project: Project, fileName: String) : JComponent(), Disposable {
  val zhaba: Image = ImageLoader.loadFromResource("/images/$fileName.png", this.javaClass)!!

  val dimension: Dimension
    get() = Dimension(zhaba.getWidth(null), zhaba.getHeight(null))

  lateinit var zhabaPoint: RelativePoint

  override fun paintComponent(g: java.awt.Graphics) {
    val g2d = g as Graphics2D
    UIUtil.drawImage(g2d,
      zhaba,
      zhabaPoint.getPoint(this).x,
      zhabaPoint.getPoint(this).y,
      null)
    super.paintComponent(g)
  }

  override fun dispose() {
    val frame = WindowManager.getInstance().getFrame(project)!!
    frame.layeredPane.remove(frame.layeredPane.components.find { it == this })
  }
}

