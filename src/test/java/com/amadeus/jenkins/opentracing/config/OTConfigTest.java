package com.amadeus.jenkins.opentracing.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.OTFlowExecutionListener;
import com.amadeus.jenkins.opentracing.OTQueueListener;
import com.amadeus.jenkins.opentracing.OTRunListener;
import com.amadeus.jenkins.opentracing.SpanStorage;
import com.amadeus.jenkins.opentracing.WorkaroundGraphListener;
import com.amadeus.jenkins.opentracing.config.impl.NullConfig;
import com.amadeus.jenkins.opentracing.test.fixtures.MockTracerConf;
import hudson.ExtensionList;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.noop.NoopTracer;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class OTConfigTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void testDynamicReconfiguration() {
    SpanStorage storage = ExtensionList.lookupSingleton(SpanStorage.class);
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);

    assertThat(storage.size()).isZero();

    MockTracer lowLevelTracer1 = new MockTracer(Propagator.TEXT_MAP);
    TracerConfig tracerConfig1 = new MockTracerConf(lowLevelTracer1);
    config.setTracer(tracerConfig1);

    Tracer applicationTracer = config.getTracerForName("fooBar");
    assertThat(applicationTracer).isNotNull();
    Map<Object, Span> applicationCache = storage.getCache(new Object(), "");

    Span applicationSpan1 = applicationTracer.buildSpan("foobar").start();
    applicationCache.put(new Object(), applicationSpan1);
    assertThat(storage.size()).isEqualTo(1);
    applicationSpan1.finish();
    assertThat(storage.size()).isEqualTo(1);
    assertThat(lowLevelTracer1.finishedSpans()).hasSize(1);

    MockTracer lowLevelTracer2 = new MockTracer(Propagator.TEXT_MAP);
    TracerConfig tracerConfig2 = new MockTracerConf(lowLevelTracer2);
    config.setTracer(tracerConfig2);
    assertThat(storage.size()).isEqualTo(0);

    Span applicationSpan2 = applicationTracer.buildSpan("something").start();
    applicationCache.put(new Object(), applicationSpan2);
    assertThat(storage.size()).isEqualTo(1);
    applicationSpan2.finish();
    assertThat(storage.size()).isEqualTo(1);
    // mockspan cleans finishedSpans
    assertThat(lowLevelTracer1.finishedSpans()).hasSize(1);
    assertThat(lowLevelTracer2.finishedSpans()).hasSize(1);
  }

  @Test
  public void testConfig() throws Exception {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);

    j.configRoundtrip();

    TracerConfig original = config.getTracer();
    assertThat(original).isExactlyInstanceOf(NullConfig.class);

    j.configRoundtrip();

    TracerConfig new1 = config.getTracer();
    assertThat(new1).isExactlyInstanceOf(NullConfig.class).isNotSameAs(original);

    j.configRoundtrip();

    TracerConfig new2 = config.getTracer();
    assertThat(new2).isExactlyInstanceOf(NullConfig.class).isNotSameAs(new1).isNotSameAs(original);

    Tracer tracer = new2.getTracerForName("foo");
    assertThat(tracer).isInstanceOf(NoopTracer.class);
    Span span = tracer.buildSpan("foo").start();
    assertThat(new2.getTraceLinkAction(span)).isEmpty();
  }

  @Test
  public void testComponentCreationWithoutConfiguration() {
    OTConfig config = assertComponent(OTConfig.class);
    assertThat(config.getTracer()).isNotNull();
    assertThat(config.getTracerForName("foo")).isNotNull();
    assertComponent(OTFlowExecutionListener.class);
    assertComponent(OTQueueListener.class);
    assertComponent(OTRunListener.class);
    assertComponent(SpanStorage.class);
    assertComponent(WorkaroundGraphListener.class);
  }

  private <T> T assertComponent(Class<T> klazz) {
    T instance = j.jenkins.getExtensionList(klazz).getInstance(klazz);
    assertThat(instance).isNotNull();
    return instance;
  }
}
