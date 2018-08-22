package com.jetbrains.edu.learning.checkio.call;

import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.CallAdapter;

import java.lang.reflect.Type;

public class CheckiOCallAdapter<R> implements CallAdapter<R, CheckiOCall<R>> {
  private final Type myResponseType;

  CheckiOCallAdapter(@NotNull Type responseType) {
    myResponseType = responseType;
  }

  @Override
  public Type responseType() {
    return myResponseType;
  }

  @Override
  public CheckiOCall<R> adapt(@NotNull Call<R> call) {
    return new CheckiOCall<>(call);
  }
}
