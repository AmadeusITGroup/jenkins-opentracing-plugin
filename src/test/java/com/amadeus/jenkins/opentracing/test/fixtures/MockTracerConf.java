package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.config.TracerUiLink;
import io.opentracing.Span;
import io.opentracing.mock.MockTracer;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import org.kohsuke.stapler.DataBoundConstructor;

@ParametersAreNonnullByDefault
public class MockTracerConf extends TracerConfig {
  private transient MockTracer tracer;

  @DataBoundConstructor
  public MockTracerConf(MockTracer tracer) {
    this.tracer = tracer;
  }

  public MockTracer getTracer() {
    return tracer;
  }

  @Override
  public MockTracer getTracerForName(String name) {
    return tracer;
  }

  @Override
  public Optional<TracerUiLink> getTraceLinkAction(Span span) {
    return Optional.of(new NullTracerUiLink());
  }

  private static class NullTracerUiLink implements TracerUiLink {
    @CheckForNull
    @Override
    public String getIconFileName() {
      return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
      return null;
    }

    @CheckForNull
    @Override
    public String getUrlName() {
      return null;
    }
  }
}
