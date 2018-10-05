package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.OauthAccount;
import com.jetbrains.edu.learning.TokenInfo;
import org.jetbrains.annotations.NotNull;

public class StepicUser extends OauthAccount<StepicUserInfo> {
  private StepicUser() { }

  public StepicUser(@NotNull TokenInfo tokenInfo) {
    super(tokenInfo);
  }

  public static StepicUser createEmptyUser() {
    return new StepicUser();
  }

  @Transient
  public int getId() {
    StepicUserInfo info = getUserInfo();
    return info.getId();
  }

  @Transient
  public void setId(int id) {
    StepicUserInfo info = getUserInfo();
    info.setId(id);
  }

  @Transient
  public String getFirstName() {
    StepicUserInfo info = getUserInfo();
    return info.getFirstName();
  }

  @Transient
  public void setFirstName(final String firstName) {
    StepicUserInfo info = getUserInfo();
    info.setFirstName(firstName);
  }

  @Transient
  public String getLastName() {
    StepicUserInfo info = getUserInfo();
    return info.getLastName();
  }

  @Transient
  public void setLastName(final String lastName) {
    StepicUserInfo info = getUserInfo();
    info.setLastName(lastName);
  }

  @Transient
  @NotNull
  public String getName() {
    StepicUserInfo info = getUserInfo();
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

  @Transient
  public boolean isGuest() {
    StepicUserInfo userInfo = getUserInfo();
    return userInfo.isGuest();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    StepicUser user = (StepicUser)o;
    StepicUserInfo info = getUserInfo();
    StepicUserInfo otherInfo = user.getUserInfo();
    return info.equals(otherInfo);
  }

  @Override
  public int hashCode() {
    StepicUserInfo info = getUserInfo();
    return info.hashCode();
  }
}
