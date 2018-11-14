package com.jetbrains.edu.learning.checkio.call;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class CheckiOCallAdapterFactory extends CallAdapter.Factory {

  @Nullable
  @Override
  public CallAdapter<?, ?> get(@NotNull Type returnType, @NotNull Annotation[] annotations, @NotNull Retrofit retrofit) {
    if (getRawType(returnType) != CheckiOCall.class) {
      return null;
    } else if (!(returnType instanceof ParameterizedType)) {
      throw new IllegalStateException("CheckiOCall must be parametrized");
    }

    final Type innerType = getParameterUpperBound(0, (ParameterizedType) returnType);
    return new CheckiOCallAdapter<>(innerType);
  }
}
