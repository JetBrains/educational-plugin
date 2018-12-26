package com.jetbrains.edu.learning.checkio.account;

import com.intellij.util.xmlb.XmlSerializer;
import com.jetbrains.edu.learning.authUtils.OAuthAccount;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.authUtils.OAuthAccountKt.deserializeAccount;

public class CheckiOAccount extends OAuthAccount<CheckiOUserInfo> {

  @SuppressWarnings("unused") // used for deserialization
  private CheckiOAccount() { }

  public CheckiOAccount(CheckiOUserInfo info, TokenInfo tokens) {
    setTokenInfo(tokens);
    setUserInfo(info);
  }

  public Element serializeIntoService(@NotNull String serviceName) {
    Element mainElement = new Element(serviceName);
    XmlSerializer.serializeInto(this, mainElement);
    Element userElement = serialize();
    mainElement.addContent(userElement);
    return mainElement;
  }

  public static CheckiOAccount fromElement(@NotNull Element element) {
    Element user = element.getChild(CheckiOAccount.class.getSimpleName());
    return deserializeAccount(user, CheckiOAccount.class, CheckiOUserInfo.class);
  }

}
