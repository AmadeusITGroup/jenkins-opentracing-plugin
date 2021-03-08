package com.amadeus.jenkins.opentracing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig;
import com.amadeus.jenkins.opentracing.config.impl.NullConfig;
import com.amadeus.jenkins.opentracing.test.TestResourceLoader;
import hudson.ExtensionList;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.yaml.YamlSource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ConfigurationAsCodeTest {
  @Rule public JenkinsRule j = new JenkinsRule();
  @Rule public TestResourceLoader configuration = new TestResourceLoader("yaml");

  private TracerConfig config;

  @Before
  public void setUp() throws Exception {
    ConfigurationAsCode.get()
        .configureWith(
            new YamlSource<>(
                configuration.getResourceAsStream(), YamlSource.READ_FROM_INPUTSTREAM));
    config = ExtensionList.lookupSingleton(OTConfig.class).getTracer();
  }

  @Test
  public void noConfig() {
    assertThat(config).isExactlyInstanceOf(NullConfig.class);
  }

  @Test
  public void nullConfig() {
    assertThat(config).isExactlyInstanceOf(NullConfig.class);
  }

  @Test
  public void jaegerNullConfig() {
    assertThat(config).isExactlyInstanceOf(JaegerConfig.class);
    JaegerConfig jaeger = (JaegerConfig) config;
    assertThat(jaeger.getSender()).isExactlyInstanceOf(JaegerConfig.NullSenderConfig.class);
  }

  @Test
  public void jaegerUdpConfig() {
    assertThat(config).isExactlyInstanceOf(JaegerConfig.class);
    JaegerConfig jaeger = (JaegerConfig) config;
    assertThat(jaeger.getUi()).isEqualTo("http://localhost/ui/");
    assertThat(jaeger.getSender()).isExactlyInstanceOf(JaegerConfig.UdpSenderConfig.class);
    JaegerConfig.UdpSenderConfig udpConfig = (JaegerConfig.UdpSenderConfig) jaeger.getSender();
    assertThat(udpConfig.getPort()).isEqualTo(1234);
    assertThat(udpConfig.getHost()).isEqualTo("someHost");
  }

  @Test
  public void jaegerHttpConfig() {
    assertThat(config).isExactlyInstanceOf(JaegerConfig.class);
    JaegerConfig jaeger = (JaegerConfig) config;
    assertThat(jaeger.getUi()).isEqualTo("http://localhost/ui/");
    assertThat(jaeger.getSender()).isExactlyInstanceOf(JaegerConfig.HttpSenderConfig.class);
    JaegerConfig.HttpSenderConfig httpConfig = (JaegerConfig.HttpSenderConfig) jaeger.getSender();
    assertThat(httpConfig.getEndpoint()).isEqualTo("http://localhost/endpoint/");
  }
}
