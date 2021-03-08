package com.amadeus.jenkins.opentracing.config;

import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.Optional;
import javax.annotation.Nonnull;

/**
 * Abstraction over configurations of different types of OpenTracing implementations Each
 * implementation is responsible for its own configuration UI.
 */
public abstract class TracerConfig extends AbstractDescribableImpl<TracerConfig>
    implements ExtensionPoint, Describable<TracerConfig> {

  /**
   * Fetch a tracer used by a component of Jenkins
   *
   * @param name component name
   */
  public abstract Tracer getTracerForName(String name);

  /**
   * Create a Jenkins action to associated with the build. Can provide a link in the UI for users to
   * directly access the trace of this build.
   *
   * @param span Span that is associated with a build
   */
  public abstract Optional<TracerUiLink> getTraceLinkAction(Span span);

  public static class ConfigDescriptor extends Descriptor<TracerConfig> {
    private String name;

    public ConfigDescriptor(String name) {
      this.name = name;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return name;
    }
  }
}
