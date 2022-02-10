package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TermsOfAgreementDialog(
    private val currentTermsOfAgreement: String,
    private val registrationLink: String,
    private val isTeamRegistrationAvailable: Boolean
) : DialogWrapper(false) {
  init {
    title = EduCoreBundle.message("codeforces.contest.registration")
    setOKButtonText(EduCoreBundle.message("codeforces.register.as.individual"))
    init()
  }

  override fun createCenterPanel(): JComponent {
    val text = if (currentTermsOfAgreement == CodeforcesNames.DEFAULT_TERMS_OF_AGREEMENT) CodeforcesNames.PRETTY_TERMS_OF_AGREEMENT
    else currentTermsOfAgreement.replace("\n", "<br>")

    val panel = CustomTextPanel("ul li {list-style-type:disc;margin-top: 12px;}").apply {
      setBody(text)
      border = JBUI.Borders.empty(10, 10)
    }
    return panel
  }

  override fun createNorthPanel(): JComponent? {
    if (!isTeamRegistrationAvailable) return super.createNorthPanel()
    val textPanel = CustomTextPanel().apply {
      setBody(EduCoreBundle.message("codeforces.team.registration.notice", registrationLink))
      border = JBUI.Borders.empty(10, 10, 12, 16)
    }
    val jPanel = JPanel(BorderLayout())
    jPanel.add(textPanel, BorderLayout.CENTER)
    jPanel.add(JSeparator(), BorderLayout.SOUTH)
    return jPanel
  }

  private class CustomTextPanel(private val style: String = "") : HtmlPanel() {
    override fun getBody(): String {
      return ""
    }

    override fun setBody(text: String) {
      setText("""
        <html>
        <head>
          <style>
            body {width: 380px}
            $style
          </style>
        </head>
        <body>
        $text
        </body>
        </html>
      """.trimIndent())
    }
  }

}