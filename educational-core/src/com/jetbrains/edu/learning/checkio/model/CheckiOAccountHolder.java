package com.jetbrains.edu.learning.checkio.model;

import org.jetbrains.annotations.NotNull;

public interface CheckiOAccountHolder {
  @NotNull
  CheckiOAccount getAccount();

  void setAccount(@NotNull CheckiOAccount account);
}
