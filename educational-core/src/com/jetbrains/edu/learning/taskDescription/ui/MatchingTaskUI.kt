package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.ui.JBColor
import com.jetbrains.edu.learning.taskDescription.isNewUI
import java.awt.Color


// this class should disappear after adding named colors
object MatchingTaskUI {
  object Key {
    fun foreground(): JBColor {
      return if (isNewUI()) {
        JBColor(Color(0x000000), Color(0xDFE1E5))
      }
      else {
        JBColor(Color(0x000000), Color(0xBBBBBB))
      }
    }

    fun background(): JBColor {
      return if (isNewUI()) {
        JBColor(Color(0xDFE1E5), Color(0x43454A))
      }
      else {
        JBColor(Color(0xF2F2F2), Color(0x4C5052))
      }
    }
  }
  object Value {
    fun foreground(): JBColor {
      return if (isNewUI()) {
        JBColor(Color(0x000000), Color(0xDFE1E5))
      } else {
        JBColor(Color(0x000000), Color(0xBBBBBB))
      }
    }

    fun background(): JBColor {
      return if (isNewUI()) {
        JBColor(Color(0xFFFFFF), Color(0x2B2D30))
      } else {
        JBColor(Color(0xFFFFFF), Color(0x3C3F41))
      }
    }

    fun borderColor(): JBColor {
      return if (isNewUI()) {
        JBColor(Color(0xC9CCD6), Color(0x4E5157))
      } else {
        JBColor(Color(0xD1D1D1), Color(0x5E6060))
      }
    }
  }
}