package com.jetbrains.edu.learning.statistics;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.jetbrains.edu.learning.serialization.SerializationUtils;
import gnu.trove.TObjectIntHashMap;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
  name = "Edu.Statistics",
  storages = @Storage(value = "edu.stats.xml", roamingType = RoamingType.DISABLED)
)
public class EduStatistics implements PersistentStateComponent<Element> {
  private static final String DESCRIPTORS = "descriptors";
  private TObjectIntHashMap<String> myUsageDescriptors = new TObjectIntHashMap<>();

  @Nullable
  @Override
  public Element getState() {
    Element descriptors = new Element(DESCRIPTORS);
    myUsageDescriptors.forEachEntry((a, b) -> {
      SerializationUtils.Xml.addChildWithName(descriptors, a, b);
      return true;
    });
    return descriptors;
  }

  @Override
  public void loadState(@NotNull Element state) {
    for (Element element : state.getChildren()) {
      String key = element.getAttributeValue(SerializationUtils.Xml.NAME);
      Integer value = Integer.valueOf(element.getAttributeValue(SerializationUtils.Xml.VALUE));
      myUsageDescriptors.put(key, value);
    }
  }

  public TObjectIntHashMap<String> getUsageDescriptors() {
    return myUsageDescriptors;
  }
}
