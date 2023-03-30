package com.jetbrains.edu.javascript.learning;

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;

public class JsNewProjectSettings {
  private NodeJsInterpreter mySelectedInterpreter;

  public NodeJsInterpreter getSelectedInterpreter() {
    return mySelectedInterpreter;
  }

  public void setSelectedInterpreter(NodeJsInterpreter selectedInterpreter) {
    mySelectedInterpreter = selectedInterpreter;
  }
}
