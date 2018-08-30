package com.jetbrains.edu.jbserver

import com.intellij.ide.AppLifecycleListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.ActionCallback
import com.intellij.openapi.util.Ref
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.jetbrains.edu.learning.EduSettings


abstract class IntervalNotifier(parentDisposable: Disposable, val checkInterval: Long = DateFormatUtil.DAY) {

  private val checkRunnable = Runnable { action().doWhenDone { queueNextCheck(checkInterval) } }
  private val checkForNotifyAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable)

  fun scheduleNotification() {
    ApplicationManager.getApplication().messageBus.connect().subscribe(AppLifecycleListener.TOPIC, object : AppLifecycleListener {
      override fun appFrameCreated(commandLineArgs: Array<String>?, willOpenProject: Ref<Boolean>) {
        scheduleNotificationInternal()
      }
    })
  }

  fun scheduleNotificationInternal() {
    val timeBeforeNextCheck = EduSettings.getInstance().lastTimeChecked + checkInterval - System.currentTimeMillis()
    return if (timeBeforeNextCheck <= 0) {
      checkRunnable.run()
    } else {
      queueNextCheck(timeBeforeNextCheck)
    }
  }

  private fun queueNextCheck(interval: Long) {
    checkForNotifyAlarm.addRequest(checkRunnable, interval)
  }

  abstract fun action(): ActionCallback

}