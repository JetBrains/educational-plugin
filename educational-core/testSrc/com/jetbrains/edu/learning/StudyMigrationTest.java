package com.jetbrains.edu.learning;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.JdomKt;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import com.jetbrains.edu.learning.serialization.StudyUnrecognizedFormatException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class StudyMigrationTest extends LightPlatformCodeInsightFixtureTestCase {

  public void testFromThirdToForth() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(3);
  }

  public void testAdaptive45() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(4);
  }

  public void testSubtasks45() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(4);
  }

  public void testTheory35To4() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(4);
  }

  public void testTheory351To4() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(4);
  }

  public void testPycharmToEdu() throws JDOMException, IOException, StudyUnrecognizedFormatException {
    doTest(7);
  }

  private void doTest(int version) throws IOException, JDOMException, StudyUnrecognizedFormatException {
    final String name = getTestName(true);
    final Path before = Paths.get(getTestDataPath()).resolve(name + ".xml");
    final Path after = Paths.get(getTestDataPath()).resolve(name + ".after.xml");
    Element element = JdomKt.loadElement(before);
    Element converted = element;
    switch (version) {
      case 1:
        converted = SerializationUtils.Xml.convertToSecondVersion(getProject(), element);
        break;
      case 3:
        converted = SerializationUtils.Xml.convertToFourthVersion(getProject(), element);
        break;
      case 4:
        converted = SerializationUtils.Xml.convertToFifthVersion(getProject(), element);
        break;
      case 7:
        converted = SerializationUtils.Xml.convertToSeventhVersion(getProject(), element);
        break;
    }
    assertTrue(JDOMUtil.areElementsEqual(converted, JdomKt.loadElement(after)));
  }

  @NotNull
  protected String getTestDataPath() {
    return "testData/migration";
  }
}
