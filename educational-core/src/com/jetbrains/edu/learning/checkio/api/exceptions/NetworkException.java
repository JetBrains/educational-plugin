package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.api.CheckiORetrofitExtKt;
import com.jetbrains.edu.learning.messages.EduCoreBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * This exception is thrown when network error occurred,
 * e.g. internet connection is disabled or endpoint doesn't respond
 *
 * @see CheckiORetrofitExtKt#executeHandlingCheckiOExceptions(retrofit2.Call)
 * */
public class NetworkException extends ApiException {
  public NetworkException() {
    this(EduCoreBundle.message("exception.message.connection.failed"));
  }

  public NetworkException(@NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String message) {
    super(message);
  }

  public NetworkException(@NotNull IOException e) {
    super(e);
  }
}
