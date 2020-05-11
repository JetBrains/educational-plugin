package com.jetbrains.edu.learning.settings

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector
import com.jetbrains.edu.learning.taskDescription.ui.createTextPane
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

// Implement this class to show oauth settings with login/logout link
abstract class OauthOptions<T : OAuthAccount<out Any>> : OptionsProvider {
  private var browseProfileLabel = createTextPane()
  private var loginLink = HoverHyperlinkLabel("")
  private var accountPanel = JPanel()
  private var mainPanel: JPanel = JPanel()
  private var loginListener: HyperlinkAdapter? = null

  protected var lastSavedAccount: T? = null
  private var initialAccount: T? = null

  abstract fun getCurrentAccount(): T?
  abstract fun setCurrentAccount(account: T?)
  abstract fun getProfileUrl(userInfo: Any): String
  protected abstract fun createAuthorizeListener(): LoginListener

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
      setCurrentAccount(initialAccount)
      updateLoginLabels()
    }
  }

  protected fun updateLoginLabels() {
    if (loginListener != null) {
      loginLink.removeHyperlinkListener(loginListener)
    }

    if (lastSavedAccount == null) {
      browseProfileLabel.text = "You're not logged in"
      loginLink.text = "Log in to $displayName"

      loginListener = createAuthorizeListener()
    }
    else {
      val info = lastSavedAccount!!.userInfo
      browseProfileLabel.text = "You're logged in as <a href=${getProfileUrl(info)}>${info}</a>"
      loginLink.text = "Log out"
      loginListener = createLogoutListener()
    }

    loginLink.addHyperlinkListener(loginListener)
  }

  protected open fun createLogoutListener(): HyperlinkAdapter {
    return object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        lastSavedAccount = null
        setCurrentAccount(null)
        updateLoginLabels()
        EduCounterUsageCollector.loggedOut(displayName, EduCounterUsageCollector.AuthorizationPlace.SETTINGS)
      }
    }
  }

  abstract inner class LoginListener : HyperlinkAdapter() {
    protected abstract fun authorize(e: HyperlinkEvent?)

    override fun hyperlinkActivated(e: HyperlinkEvent?) {
      authorize(e)
      EduCounterUsageCollector.loggedIn(displayName, EduCounterUsageCollector.AuthorizationPlace.SETTINGS)
    }
  }
}
