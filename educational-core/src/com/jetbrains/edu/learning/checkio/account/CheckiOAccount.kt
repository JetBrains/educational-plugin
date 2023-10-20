package com.jetbrains.edu.learning.checkio.account

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.XmlSerializer
import com.jetbrains.edu.learning.authUtils.OAuthAccount
import com.jetbrains.edu.learning.authUtils.TokenInfo
import com.jetbrains.edu.learning.authUtils.deserializeOAuthAccount
import com.jetbrains.edu.learning.authUtils.serialize
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CHECKIO
import org.jdom.Element
import org.jetbrains.annotations.NonNls

class CheckiOAccount : OAuthAccount<CheckiOUserInfo> {
  @Suppress("unused") // used for deserialization
  private constructor()
  constructor(tokens: TokenInfo) : super(tokens.expiresIn)

  fun serializeIntoService(serviceName: @NonNls String): Element {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = serialize()
    mainElement.addContent(userElement)
    return mainElement
  }

  @Suppress("UnstableApiUsage")
  override val servicePrefix: @NlsSafe String
    get() = CHECKIO

  override fun getUserName(): String = userInfo.getFullName()

  companion object {
    fun fromElement(element: Element): CheckiOAccount? {
      val user = element.getChild(CheckiOAccount::class.java.simpleName)
      val account = deserializeOAuthAccount(user, CheckiOAccount::class.java, CheckiOUserInfo::class.java)

      // We've changed CheckiO deserialization in 2022.1 version. It causes invalid deserialization of already existed accounts,
      // so we force user to do re-login.
      return if (account?.getUserName()?.isNotEmpty() == true) account
      else null
    }
  }
}
