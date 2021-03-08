package com.amadeus.jenkins.opentracing;

import static org.assertj.core.api.Java6Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig;
import com.amadeus.jenkins.opentracing.test.TestResourceLoader;
import hudson.ExtensionList;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.RestartableJenkinsRule;

public class RestartTest {
  @Rule public RestartableJenkinsRule r = new RestartableJenkinsRule();
  @Rule public TestResourceLoader script = new TestResourceLoader("groovy");

  @Test
  public void testConfigurationSurvivesRestart() {
    AtomicReference<OTConfig> originalConfig = new AtomicReference<>();

    r.then(
        j -> {
          OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
          config.setTracer(new JaegerConfig(null, null));
          originalConfig.set(config);
          j.configRoundtrip();
        });
    r.then(
        j -> {
          OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
          assertThat(originalConfig.get()).isNotNull().isNotSameAs(config);
          assertThat(config.getTracer()).isExactlyInstanceOf(JaegerConfig.class);
        });
  }
}
