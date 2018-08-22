package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.call.CheckiOCall;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

/**
 * It's thrown when error occurred parsing Json object to Java object
 *
 * @see CheckiOCall#execute()
 * */
public class ParseException extends ApiException {
  public ParseException(@NotNull Response rawResponse) {
    super(rawResponse.toString());
  }
}
