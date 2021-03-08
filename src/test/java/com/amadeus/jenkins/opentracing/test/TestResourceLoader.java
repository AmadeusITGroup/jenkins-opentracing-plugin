package com.amadeus.jenkins.opentracing.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

@SuppressWarnings("WeakerAccess")
public class TestResourceLoader extends TestWatcher {
  private String suffix;

  private String className, methodName;

  public TestResourceLoader(String suffix) {
    this.suffix = suffix;
  }

  @Override
  protected void starting(Description d) {
    this.className = d.getClassName();
    this.methodName = d.getMethodName();
  }

  private String resourceName() {
    return (className + '.' + methodName).replace('.', '/') + '.' + suffix;
  }

  public InputStream getResourceAsStream() {
    InputStream result = ClassLoader.getSystemResourceAsStream(resourceName());
    if (result == null) {
      throw new RuntimeException("Could not find " + resourceName());
    }
    return result;
  }

  public String getResourceAsString() {
    try {
      return IOUtils.toString(getResourceAsStream(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public String read() {
    return getResourceAsString();
  }
}
