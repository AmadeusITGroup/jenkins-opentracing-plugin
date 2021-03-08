package com.amadeus.jenkins.opentracing.config.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig.NullSenderConfig;
import hudson.ExtensionList;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMapAdapter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JaegerConfigTest {

  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void testBasic() throws Exception {

    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    JaegerConfig jaegerConfig = new JaegerConfig(null, "uiUrl");
    config.setTracer(jaegerConfig);

    j.configRoundtrip();

    TracerConfig tracerConfig = config.getTracer();
    assertThat(tracerConfig).isExactlyInstanceOf(JaegerConfig.class).isNotSameAs(jaegerConfig);
    JaegerConfig newConfig = (JaegerConfig) tracerConfig;

    assertThat(newConfig.getUi()).isEqualTo("uiUrl");
    assertThat(newConfig.getSender())
        .isNotNull()
        .isExactlyInstanceOf(JaegerConfig.NullSenderConfig.class);

    Span span = newConfig.getTracerForName("foo").buildSpan("foo").start();
    assertThat(newConfig.getTraceLinkAction(span))
        .hasValueSatisfying(
            uiLink -> {
              assertThat(uiLink.getDisplayName()).isEqualTo("Jaeger UI");
              assertThat(uiLink.getUrlName()).startsWith("uiUrl/trace/");
              assertThat(uiLink.getIconFileName())
                  .isEqualTo("/plugin/opentracing/images/16x16/jaeger.png");
            });
  }

  @Test
  public void testNullSender() throws Exception {
    assertSender(JaegerConfig.NullSenderConfig.class, new JaegerConfig.NullSenderConfig());
  }

  @Test
  public void testUdpSender() throws Exception {
    JaegerConfig.UdpSenderConfig udpSender =
        assertSender(
            JaegerConfig.UdpSenderConfig.class, new JaegerConfig.UdpSenderConfig("someHost", 42));
    assertThat(udpSender.getHost()).isEqualTo("someHost");
    assertThat(udpSender.getPort()).isEqualTo(42);
  }

  @Test
  public void testHttpSender() throws Exception {
    JaegerConfig.HttpSenderConfig httpSender =
        assertSender(
            JaegerConfig.HttpSenderConfig.class, new JaegerConfig.HttpSenderConfig("someEndpoint"));
    assertThat(httpSender.getEndpoint()).isEqualTo("someEndpoint");
  }

  private <T extends JaegerConfig.SenderConfig> T assertSender(Class<? extends T> cls, T sender)
      throws Exception {

    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);

    JaegerConfig jaegerConfig = new JaegerConfig(sender, null);
    config.setTracer(jaegerConfig);

    j.configRoundtrip();

    TracerConfig tracerConfig = config.getTracer();
    assertThat(tracerConfig).isExactlyInstanceOf(JaegerConfig.class).isNotSameAs(jaegerConfig);

    JaegerConfig.SenderConfig senderConfig = ((JaegerConfig) tracerConfig).getSender();
    assertThat(senderConfig).isExactlyInstanceOf(cls).isNotSameAs(sender);

    T typedSenderConfig = cls.cast(senderConfig);
    assertThat(typedSenderConfig).isNotNull();

    return typedSenderConfig;
  }

  @Test
  public void testNoUnderscoreIsInjected() {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    JaegerConfig jaegerConfig = new JaegerConfig(new NullSenderConfig(), null);
    config.setTracer(jaegerConfig);
    Tracer tracer = config.getTracerForName("foo");
    Span span = tracer.buildSpan("foo").start();
    Map<String, String> env = new HashMap<>();
    tracer.inject(span.context(), Builtin.TEXT_MAP, new TextMapAdapter(env));
    assertThat(env.keySet())
        .isNotEmpty()
        .noneMatch(k -> k.contains("-"))
        .allMatch(k -> k.contains("_")); // may not hold true forever
  }
}
