package com.jetbrains.edu.learning.checkio.api.exceptions;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class ParseException extends ApiException {
  public ParseException(@NotNull Response rawResponse) {
    super(rawResponse.toString());
  }
}
