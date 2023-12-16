package com.jetbrains.edu.learning.codeforces.newProjectUI

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

class TermsOfAgreementDialog(
  private val currentTermsOfAgreement: String,
  private val registrationLink: String,
  private val isTeamRegistrationAvailable: Boolean
) : DialogWrapper(false) {

  private val prettyTermsOfAgreement = EduCoreBundle.message("codeforces.default.terms.of.agreement",
                                                             CodeforcesNames.CONTESTS_POST,
                                                             CodeforcesNames.CONTESTS_RULES,
                                                             CodeforcesNames.THIRD_PARTY_CODE_RULE_CHANGING)

  init {
    title = EduCoreBundle.message("codeforces.contest.registration")
    setOKButtonText(EduCoreBundle.message("codeforces.register.as.individual"))
    init()
  }

  override fun createCenterPanel(): JComponent {
    val text = if (currentTermsOfAgreement == DEFAULT_TERMS_OF_AGREEMENT) prettyTermsOfAgreement
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
      @NonNls
      val formattedText = """
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
      """.trimIndent()
      setText(formattedText)
    }
  }

  companion object {
    private const val DEFAULT_TERMS_OF_AGREEMENT = "The registration confirms that you:\n" +
                                           "\n" +
                                           "* have read the contest rules  by the links http://codeforces.com/blog/entry/456 and http://codeforces.com/blog/entry/4088\n" +
                                           "* will not violate the rules described on http://codeforces.com/blog/entry/456 and/or http://codeforces.com/blog/entry/4088\n" +
                                           "* will not communicate with other participants, share ideas of solutions and hacks\n" +
                                           "* will not use third-party code, except stated in http://codeforces.com/blog/entry/8790\n" +
                                           "* will not attempt to deliberately destabilize the testing process and try to hack the contest system in any form\n" +
                                           "* will not use multiple accounts and will take part in the contest using your personal and the single account."

  }

}