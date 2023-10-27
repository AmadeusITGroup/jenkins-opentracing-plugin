package com.amadeus.jenkins.opentracing.config.impl;

import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.config.TracerUiLink;
import hudson.Extension;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopTracerFactory;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/** Implementation of {@link TracerConfig} that does nothing. */
@Restricted(NoExternalUse.class)
public final class NullConfig extends TracerConfig {

  @DataBoundConstructor
  public NullConfig() {
    /* for injection */
  }

  @Override
  public Tracer getTracerForName(String name) {
    return NoopTracerFactory.create();
  }

  @Override
  public Optional<TracerUiLink> getTraceLinkAction(@Nonnull Span span) {
    return Optional.empty();
  }

  @Extension(ordinal = 100)
  @Symbol("null")
  public static final class DescriptorImpl extends TracerConfig.ConfigDescriptor {

    public DescriptorImpl() {
      super("Disabled");
    }
  }
}
