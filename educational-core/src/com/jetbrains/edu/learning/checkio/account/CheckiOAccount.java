package com.jetbrains.edu.learning.checkio.account;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.learning.authUtils.OAuthAccount;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import com.jetbrains.edu.learning.checkio.utils.CheckiONames;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.authUtils.OAuthAccountKt.deserializeOAuthAccount;

public class CheckiOAccount extends OAuthAccount<CheckiOUserInfo> {

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOAccount() { }

  public CheckiOAccount(CheckiOUserInfo info, TokenInfo tokens) {
    super(tokens.getExpiresIn());
    setUserInfo(info);
    saveTokens(tokens);
  }

  public Element serializeIntoService(@NotNull @NonNls String serviceName) {
    Element mainElement = new Element(serviceName);
    XmlSerializer.serializeInto(this, mainElement);
    Element userElement = serialize();
    mainElement.addContent(userElement);
    return mainElement;
  }

  @NotNull
  @Override
  @NlsSafe
  public String getServicePrefix() {
    return CheckiONames.CHECKIO;
  }

  @NotNull
  @Override
  protected String getUserName() {
    return userInfo.getUsername();
  }

  public static CheckiOAccount fromElement(@NotNull Element element) {
    Element user = element.getChild(CheckiOAccount.class.getSimpleName());
    return deserializeOAuthAccount(user, CheckiOAccount.class, CheckiOUserInfo.class);
  }
}
