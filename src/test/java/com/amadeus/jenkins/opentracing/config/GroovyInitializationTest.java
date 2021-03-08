package com.amadeus.jenkins.opentracing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig;
import hudson.ExtensionList;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.LocalData;

public class GroovyInitializationTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  @LocalData
  public void testGroovyInitialization() {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    TracerConfig tracerConfig = config.getTracer();
    assertThat(tracerConfig).isExactlyInstanceOf(JaegerConfig.class);
    JaegerConfig jaegerConfig = (JaegerConfig) tracerConfig;
    assertThat(jaegerConfig.getUi()).isEqualTo("https://localhost:5678");
    JaegerConfig.UdpSenderConfig sender = (JaegerConfig.UdpSenderConfig) jaegerConfig.getSender();
    assertThat(sender.getHost()).isEqualTo("localhost");
    assertThat(sender.getPort()).isEqualTo(1234);
  }
}
