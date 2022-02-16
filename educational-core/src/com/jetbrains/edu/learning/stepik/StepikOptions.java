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

import com.jetbrains.edu.learning.api.EduOAuthConnector;
import com.jetbrains.edu.learning.settings.OAuthLoginOptions;
import com.jetbrains.edu.learning.stepik.api.StepikConnector;
import org.jetbrains.annotations.NotNull;

public class StepikOptions extends OAuthLoginOptions<StepikUser> {
  @Override
  protected @NotNull EduOAuthConnector<StepikUser, ?> getConnector() {
    return StepikConnector.getInstance();
  }

  @NotNull
  @Override
  public String getDisplayName() {
    return StepikNames.STEPIK;
  }

  @NotNull
  @Override
  protected String profileUrl(@NotNull StepikUser account) {
    return StepikUtils.getProfileUrl(account);
  }
}
