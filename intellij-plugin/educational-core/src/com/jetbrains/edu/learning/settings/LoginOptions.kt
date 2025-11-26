package com.jetbrains.edu.learning.settings

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.authUtils.Account
import com.jetbrains.edu.learning.courseFormat.UserInfo
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.taskToolWindow.ui.createTextPane
import javax.swing.JComponent
import javax.swing.JPanel

// Implement this class to show oauth settings with login/logout link
abstract class LoginOptions<T : Account<out UserInfo>> : OptionsProvider {
  private var browseProfileLabel = createTextPane()
  private var loginLink = HoverHyperlinkLabel("")
  private var accountPanel = JPanel()
  private var mainPanel: JPanel = JPanel()
  private var loginListener: HyperlinkAdapter? = null

  protected var lastSavedAccount: T? = null
  private var initialAccount: T? = null

  abstract fun getCurrentAccount(): T?

  abstract fun setCurrentAccount(account: T?)

  protected abstract fun profileUrl(account: T): String

  protected abstract fun createAuthorizeListener(): HyperlinkAdapter

  protected abstract fun createLogOutListener(): HyperlinkAdapter?

  private fun initUI() {
    val additionalComponents = getAdditionalComponents()
    mainPanel = JPanel(GridLayoutManager(additionalComponents.size + 1, 1))
    mainPanel.border = IdeBorderFactory.createTitledBorder(displayName)

    accountPanel = JPanel(GridLayoutManager(1, 2))
    addBrowseProfileLabel()
    addLoginLink()

    fun gridConstraints(row: Int): GridConstraints {
      return GridConstraints().apply {
        this.row = row
        column = 0
        anchor = GridConstraints.ANCHOR_WEST
      }
    }

    mainPanel.add(accountPanel, gridConstraints(0))
    additionalComponents.forEachIndexed { index, component -> mainPanel.add(component, gridConstraints(index + 1)) }
  }

  protected open fun getAdditionalComponents(): List<JComponent> = emptyList()

  private fun initAccounts() {
    initialAccount = getCurrentAccount()
    lastSavedAccount = getCurrentAccount()
  }

  private fun addBrowseProfileLabel() {
    browseProfileLabel = createTextPane()
    browseProfileLabel.background = UIUtil.getPanelBackground()
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 0
    constraints.anchor = GridConstraints.ANCHOR_WEST
    constraints.hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
    browseProfileLabel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    accountPanel.add(browseProfileLabel, constraints)
  }

  private fun addLoginLink() {
    loginLink = HoverHyperlinkLabel("")
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 1
    constraints.anchor = GridConstraints.ANCHOR_WEST
    accountPanel.add(loginLink, constraints)
  }

  override fun createComponent(): JComponent? {
    initUI()
    initAccounts()
    return mainPanel
  }

  override fun isModified(): Boolean {
    return getCurrentAccount() != initialAccount
  }

  override fun reset() {
    if (lastSavedAccount != initialAccount) {
      setCurrentAccount(initialAccount)
    }
    lastSavedAccount = initialAccount
    updateLoginLabels()
  }

  override fun apply() {
    if (isModified) {
      initialAccount = lastSavedAccount
      if (getCurrentAccount() != initialAccount) {
        setCurrentAccount(initialAccount)
      }
      updateLoginLabels()
    }
  }

  protected fun updateLoginLabels() {
    if (loginListener != null) {
      loginLink.removeHyperlinkListener(loginListener)
    }

    val selectedAccount = lastSavedAccount

    if (selectedAccount == null) {
      browseProfileLabel.text = EduCoreBundle.message("not.logged.in")
      loginLink.text = EduCoreBundle.message("log.in.to", displayName)
      loginListener = createAuthorizeListener()
    }
    else {
      val info = selectedAccount.userInfo
      browseProfileLabel.text = EduCoreBundle.message("logged.in.as.verbose", "<a href=${profileUrl(selectedAccount)}>${info}</a>")
      loginLink.text = getLogoutText()
      loginListener = createLogOutListener()
    }

    loginLink.addHyperlinkListener(loginListener)
  }

  open fun getLogoutText(): String = EduCoreBundle.message("log.out")
}
