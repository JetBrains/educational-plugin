package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.xmlb.annotations.Transient;
import com.jetbrains.edu.learning.authUtils.OAuthAccount;
import com.jetbrains.edu.learning.authUtils.TokenInfo;
import org.jetbrains.annotations.NotNull;

public class StepikUser extends OAuthAccount<StepikUserInfo> {
  private StepikUser() { }

  public StepikUser(@NotNull TokenInfo tokenInfo) {
    super(tokenInfo.getExpiresIn());
  }

  @NotNull
  @Override
  @NlsSafe
  public String getServicePrefix() {
    return StepikNames.STEPIK;
  }

  @NotNull
  @Override
  protected String getUserName() {
    return userInfo.getFullName();
  }

  public static StepikUser createEmptyUser() {
    return new StepikUser();
  }

  @Transient
  public int getId() {
    StepikUserInfo info = getUserInfo();
    return info.id;
  }

  @Transient
  public String getFirstName() {
    StepikUserInfo info = getUserInfo();
    return info.getFirstName();
  }

  @Transient
  public String getLastName() {
    StepikUserInfo info = getUserInfo();
    return info.getLastName();
  }

  @Transient
  @NotNull
  public String getName() {
    StepikUserInfo info = getUserInfo();
    return StringUtil.join(new String[]{info.getFirstName(), info.getLastName()}, " ");
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
