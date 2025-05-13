package com.jetbrains.edu.uiOnboarding

import com.intellij.util.ImageLoader
import okhttp3.internal.wait
import java.awt.Image
import java.awt.image.BufferedImage

class ZhabaAnimationData private constructor(val frames: List<Image>) {

  companion object {
    private const val FRAME_COUNT: Int = 27

    const val FRAMES_ON_GROUND_BEFORE = 4
    const val FRAMES_ON_GROUND_AFTER = 5

    const val ZHABA_X0 = 27// * 110 / 130
    const val ZHABA_X1 = 276// * 110 / 130
    const val ZHABA_Y0 = 210// * 110 / 130

    fun load(): ZhabaAnimationData {
      val frames = mutableListOf<Image>()
      for (i in 0 until FRAME_COUNT) {
        val frame = ImageLoader.loadFromResource("/images/jump/Comp 2${"%02d".format(i)}.png", this.javaClass) ?: continue
        /*frame as? BufferedImage ?: continue
        val w = frame.width * 110 / 130
        val h = frame.height * 110 / 130
        val scaled = BufferedImage(w, h, frame.type)
        scaled.createGraphics().drawImage(frame, 0, 0, w, h, null)*/
        frames.add(frame)
      }

     return ZhabaAnimationData(frames)
    }
  }
}