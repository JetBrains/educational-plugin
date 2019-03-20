/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jetbrains.edu.learning.stepik;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.HyperlinkAdapter;
import com.jetbrains.edu.learning.EduLogInListener;
import com.jetbrains.edu.learning.EduSettings;
import com.jetbrains.edu.learning.settings.OauthOptions;
import com.jetbrains.edu.learning.statistics.EduUsagesCollector;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

public class StepikOptions extends OauthOptions<StepikUser> {
  public StepikOptions() {
    initAccounts();
  }

  @Nullable
  @Override
  public StepikUser getCurrentAccount() {
    return EduSettings.getInstance().getUser();
  }

  @Override
  public void setCurrentAccount(@Nullable StepikUser lastSavedAccount) {
    EduSettings.getInstance().setUser(lastSavedAccount);
  }

  @NotNull
  protected HyperlinkAdapter createAuthorizeListener() {
    return new HyperlinkAdapter() {

      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        EduUsagesCollector.loginFromSettings();
        ApplicationManager.getApplication().getMessageBus().connect().subscribe(EduSettings.SETTINGS_CHANGED, new EduLogInListener() {
          @Override
          public void userLoggedIn() {
            StepikUser user = EduSettings.getInstance().getUser();
            setLastSavedAccount(user);
            updateLoginLabels();
          }
          @Override
          public void userLoggedOut() { }
        });
        StepikAuthorizer.doAuthorize(() -> showDialog());
      }

      private void showDialog() {
        OAuthDialog dialog = new OAuthDialog();
        if (dialog.showAndGet()) {
          final StepikUser user = EduSettings.getInstance().getUser();
          setLastSavedAccount(user);
          updateLoginLabels();
        }
      }
    };
  }

  @Override
  @NotNull
  protected HyperlinkAdapter createLogoutListener() {
    return new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        setCurrentAccount(null);
        setLastSavedAccount(null);
        updateLoginLabels();
      }
    };
  }

  @Nls
  @Override
  public String getDisplayName() {
    return "Stepik";
  }
}
