package com.jetbrains.edu.learning.checkio.api.exceptions;

import com.jetbrains.edu.learning.checkio.api.MyResponse;
import com.jetbrains.edu.learning.checkio.api.RetrofitUtils;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;

/**
 * It's thrown when error occurred parsing Json object to Java object
 *
 * @see MyResponse#createParseError(retrofit2.Response)
 * @see RetrofitUtils#getResponse(Call)
 * */
public class ParseException extends ApiException {
  public ParseException(@NotNull Response rawResponse) {
    super(rawResponse.toString());
  }
}
