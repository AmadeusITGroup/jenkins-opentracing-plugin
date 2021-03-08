package com.amadeus.jenkins.opentracing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig;
import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig.HttpSenderConfig;
import hudson.ExtensionList;
import hudson.diagnosis.OldDataMonitor;
import hudson.diagnosis.OldDataMonitor.VersionRange;
import hudson.model.Saveable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class ConfigurationMigrationTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void noPersistenceViaSerialization() throws IOException {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    config.save();

    File configFile = new File(j.jenkins.getRootDir(), config.getId() + ".xml");
    assertThat(configFile).exists();
    try (FileInputStream input = new FileInputStream(configFile)) {
      String contents = IOUtils.toString(input);
      assertThat(contents).doesNotContain("unserializable-parents");
      assertThat(contents).doesNotContain("serialization=");
    }
  }

  @Test
  @LocalData
  public void testLoadingFrom100ConfigFormat() {
    asserJaegerHttpConfiguration("http://localhost:3333/", "http://localhost:2222/");
    assertOldDataWarning(false);
  }

  private void asserJaegerHttpConfiguration(String ui, String httpEndpoiint) {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    assertThat(config.getTracer()).isExactlyInstanceOf(JaegerConfig.class);
    JaegerConfig jaegerConfig = (JaegerConfig) config.getTracer();
    assertThat(jaegerConfig.getUi()).isEqualTo(ui);
    assertThat(jaegerConfig.getSender()).isExactlyInstanceOf(HttpSenderConfig.class);
    HttpSenderConfig httpSender = (HttpSenderConfig) jaegerConfig.getSender();
    assertThat(httpSender.getEndpoint()).isEqualTo(httpEndpoiint);
  }

  private void assertOldDataWarning(boolean exists) {
    OldDataMonitor monitor = ExtensionList.lookupSingleton(OldDataMonitor.class);
    Map<Saveable, VersionRange> data = monitor.getData();
    if (exists) {
      assertThat(data).hasSize(1);
      Saveable entry = data.keySet().iterator().next();
      assertThat(entry).isExactlyInstanceOf(OTConfig.class);
    } else {
      assertThat(data).hasSize(0);
    }
  }
}
