package com.jetbrains.edu.python.learning.checkio.messages;

import com.jetbrains.edu.learning.checkio.messages.CheckiOErrorInformer;
import com.jetbrains.edu.python.learning.checkio.connectors.PyCheckiOOAuthConnector;

public class PyCheckiOErrorInformer extends CheckiOErrorInformer {
  private PyCheckiOErrorInformer() {
    super(PyCheckiOOAuthConnector.getInstance());
  }

  private static class Holder {
    private static final PyCheckiOErrorInformer INSTANCE = new PyCheckiOErrorInformer();
  }

  public static PyCheckiOErrorInformer getInstance() {
    return Holder.INSTANCE;
  }
}
