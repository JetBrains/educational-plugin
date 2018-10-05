package com.jetbrains.edu.learning.settings

import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.JBLabel
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.jetbrains.edu.learning.OauthAccount
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

abstract class OauthOptions<T : OauthAccount<out Any>> : OptionsProvider {
  private var loginLabel = JBLabel()
  private var loginLink = HoverHyperlinkLabel("")
  private var panel = JPanel()
  private var loginListener: HyperlinkAdapter? = null

  protected var lastSavedAccount: T? = null
  private var initialAccount: T? = null

  init {
    initUI()
  }

  abstract fun getCurrentAccount(): T?
  abstract fun setCurrentAccount(lastSavedAccount: T?)
  protected abstract fun createAuthorizeListener(): HyperlinkAdapter

  private fun initUI() {
    panel = JPanel(GridLayoutManager(1, 2))
    addLoginLabel()
    addLoginLink()
    panel.border = IdeBorderFactory.createTitledBorder(displayName)
  }

  protected fun initAccounts() {
    initialAccount = getCurrentAccount()
    lastSavedAccount = getCurrentAccount()
  }

  private fun addLoginLabel() {
    loginLabel = JBLabel()
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 0
    constraints.anchor = GridConstraints.ANCHOR_WEST
    constraints.hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
    panel.add(loginLabel, constraints)
  }

  private fun addLoginLink() {
    loginLink = HoverHyperlinkLabel("")
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 1
    constraints.anchor = GridConstraints.ANCHOR_WEST
    panel.add(loginLink, constraints)
  }

  override fun createComponent(): JComponent? {
    lastSavedAccount = getCurrentAccount()
    return panel
  }

  override fun isModified(): Boolean {
    return getCurrentAccount() != initialAccount
  }

  override fun reset() {
    lastSavedAccount = initialAccount
    setCurrentAccount(initialAccount)
    updateLoginLabels()
  }

  override fun apply() {
    if (isModified) {
      initialAccount = lastSavedAccount
    }
    reset()
  }

  protected fun updateLoginLabels() {
    if (loginListener != null) {
      loginLink.removeHyperlinkListener(loginListener)
    }

    if (lastSavedAccount == null) {
      loginLabel.text = "You're not logged in"
      loginLink.text = "Log in to $displayName"

      loginListener = createAuthorizeListener()
    }
    else {
      val info = lastSavedAccount!!.userInfo
      loginLabel.text = "You're logged in as " + info.toString()
      loginLink.text = "Log out"
      loginListener = createLogoutListener()
    }

    loginLink.addHyperlinkListener(loginListener)
  }

  override fun disposeUIResources() {
    setCurrentAccount(initialAccount)
  }

  protected open fun createLogoutListener(): HyperlinkAdapter {
    return object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        lastSavedAccount = null
        setCurrentAccount(null)
        updateLoginLabels()
      }
    }
  }
}
