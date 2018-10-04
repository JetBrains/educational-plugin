package com.jetbrains.edu.learning.stepik.alt

import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.jetbrains.edu.learning.settings.OptionsProvider
import org.jetbrains.annotations.Nls
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class HyperskillOptions : OptionsProvider {
  private var myLoginLabel = JBLabel()
  private var myLoginLink = HoverHyperlinkLabel("")
  private var myPanel = JPanel()
  private var myLoginListener: HyperlinkAdapter? = null

  private var myCurrentAccount: HyperskillAccount? = null
  private var myLastSavedAccount: HyperskillAccount? = null

  init {
    myCurrentAccount = HyperskillSettings.instance.account

    initUI()
  }

  private fun initUI() {
    myPanel = JPanel(GridLayoutManager(1, 2))
    addLoginLabel()
    addLoginLink()
    myPanel.border = IdeBorderFactory.createTitledBorder("Hyperskill")
  }

  private fun addLoginLabel() {
    myLoginLabel = JBLabel()
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 0
    constraints.anchor = GridConstraints.ANCHOR_WEST
    constraints.hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
    myPanel.add(myLoginLabel, constraints)
  }

  private fun addLoginLink() {
    myLoginLink = HoverHyperlinkLabel("")
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 1
    constraints.anchor = GridConstraints.ANCHOR_WEST
    myPanel.add(myLoginLink, constraints)
  }

  override fun createComponent(): JComponent? {
    myLastSavedAccount = HyperskillSettings.instance.account
    return myPanel
  }

  override fun isModified(): Boolean {
    return myCurrentAccount != myLastSavedAccount
  }

  override fun reset() {
    myCurrentAccount = myLastSavedAccount
    updateLoginLabels()
  }

  override fun apply() {
    if (isModified) {
      myLastSavedAccount = myCurrentAccount
    }

    reset()
  }

  private fun updateLoginLabels() {
    if (myLoginListener != null) {
      myLoginLink.removeHyperlinkListener(myLoginListener)
    }

    if (myCurrentAccount == null) {
      myLoginLabel.text = "You're not logged in"
      myLoginLink.text = "Log in to Hyperskill"

      myLoginListener = createAuthorizeListener()
    }
    else {
      val info = myCurrentAccount!!.userInfo
      if (info != null) {
        myLoginLabel.text = "You're logged in as " + info.fullname
        myLoginLink.text = "Log out"
        myLoginListener = createLogoutListener()
      }
    }

    myLoginLink.addHyperlinkListener(myLoginListener)
  }

  private fun createAuthorizeListener(): HyperlinkAdapter {
    return object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        HyperskillConnector.doAuthorize(Runnable {
                                          myCurrentAccount = HyperskillSettings.instance.account
                                          updateLoginLabels()
                                        })
      }
    }
  }

  override fun disposeUIResources() {
    HyperskillSettings.instance.account = myLastSavedAccount
  }

  private fun createLogoutListener(): HyperlinkAdapter {
    return object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        HyperskillSettings.instance.account = null
        myCurrentAccount = null
        updateLoginLabels()
      }
    }
  }

  @Nls
  override fun getDisplayName(): String {
    return "Hyperskill options"
  }
}
