package com.jetbrains.edu.learning;

import com.intellij.openapi.util.JDOMUtil;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.JdomKt;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;

public class StudyMigrationTest {
  @Rule public TestName name = new TestName();

  @Test
  public void testFromThirdToForth() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(3);
  }

  @Test
  public void testAdaptive45() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(4);
  }

  @Test
  public void testSubtasks45() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(4);
  }

  @Test
  public void testTheory35To4() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(4);
  }

  @Test
  public void testTheory351To4() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(4);
  }

  @Test
  public void testPycharmToEdu() throws JDOMException, IOException, SerializationUtils.StudyUnrecognizedFormatException {
    doTest(7);
  }

  private void doTest(int version) throws IOException, JDOMException, SerializationUtils.StudyUnrecognizedFormatException {
    final String name = PlatformTestUtil.getTestName(this.name.getMethodName(), true);
    final Path before = getTestDataPath().resolve(name + ".xml");
    final Path after = getTestDataPath().resolve(name + ".after.xml");
    Element element = JdomKt.loadElement(before);
    Element converted = element;
    switch (version) {
      case 1:
        converted = SerializationUtils.Xml.convertToSecondVersion(element);
        break;
      case 3:
        converted = SerializationUtils.Xml.convertToForthVersion(element);
        break;
      case 4:
        converted = SerializationUtils.Xml.convertToFifthVersion(element);
        break;
      case 7:
        converted = SerializationUtils.Xml.convertToSeventhVersion(element);
        break;
    }
    assertTrue(JDOMUtil.areElementsEqual(converted, JdomKt.loadElement(after)));
  }

  @NotNull
  protected Path getTestDataPath() {
    return Paths.get("testData/migration");
  }
}
