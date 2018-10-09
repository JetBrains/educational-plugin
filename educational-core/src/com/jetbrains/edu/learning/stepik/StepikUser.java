package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.authUtils.OAuthAccount;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import org.jetbrains.annotations.NotNull;

public class StepikUser extends OAuthAccount<StepikUserInfo> {
  private StepikUser() { }

  public StepikUser(@NotNull TokenInfo tokenInfo) {
    super(tokenInfo);
  }

  public static StepikUser createEmptyUser() {
    return new StepikUser();
  }

  @Transient
  public int getId() {
    StepikUserInfo info = getUserInfo();
    return info.getId();
  }

  @Transient
  public void setId(int id) {
    StepikUserInfo info = getUserInfo();
    info.setId(id);
  }

  @Transient
  public String getFirstName() {
    StepikUserInfo info = getUserInfo();
    return info.getFirstName();
  }

  @Transient
  public void setFirstName(final String firstName) {
    StepikUserInfo info = getUserInfo();
    info.setFirstName(firstName);
  }

  @Transient
  public String getLastName() {
    StepikUserInfo info = getUserInfo();
    return info.getLastName();
  }

  @Transient
  public void setLastName(final String lastName) {
    StepikUserInfo info = getUserInfo();
    info.setLastName(lastName);
  }

  @Transient
  @NotNull
  public String getName() {
    StepikUserInfo info = getUserInfo();
    return StringUtil.join(new String[]{info.getFirstName(), info.getLastName()}, " ");
  }

  @Transient
  @NotNull
  public String getAccessToken() {
    TokenInfo tokenInfo = getTokenInfo();
    return tokenInfo.getAccessToken();
  }

  @Transient
  public void setAccessToken(String accessToken) {
    TokenInfo tokenInfo = getTokenInfo();
    tokenInfo.setAccessToken(accessToken);
  }

  @Transient
  @NotNull
  public String getRefreshToken() {
    TokenInfo tokenInfo = getTokenInfo();
    return tokenInfo.getRefreshToken();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StepikUser user = (StepikUser)o;
    StepikUserInfo info = getUserInfo();
    StepikUserInfo otherInfo = user.getUserInfo();
    return info.equals(otherInfo);
  }

  @Override
  public int hashCode() {
    StepikUserInfo info = getUserInfo();
    return info.hashCode();
  }
}
