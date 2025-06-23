package de.fraunhofer.camunda.javaserver.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public class PropertiesService {

  private static final Properties appProps;
  private static final String ONTOLOGIES = "ontologies";

  static {
    String rootPath = Objects
        .requireNonNull(Thread.currentThread().getContextClassLoader().getResource("")).getPath();
    String appPropPath = rootPath + "app.properties";

    appProps = new Properties();
    try {
      appProps.load(new FileInputStream(appPropPath));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String getProperty(String key) {
    return appProps.getProperty(key);
  }

  public static String getOntologyListString() {
    return getProperty(ONTOLOGIES);
  }

  public static String getRestEndpoint() {
    return getProperty("rest");
  }

}
