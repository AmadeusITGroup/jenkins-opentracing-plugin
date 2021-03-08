package com.amadeus.jenkins.opentracing.config.impl;

import com.amadeus.jenkins.opentracing.Utils;
import com.amadeus.jenkins.opentracing.config.BackwardCompatConverter;
import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.config.TracerUiLink;
import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import hudson.ExtensionPoint;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.XStream2;
import io.jaegertracing.Configuration;
import io.jaegertracing.Configuration.ReporterConfiguration;
import io.jaegertracing.Configuration.SamplerConfiguration;
import io.jaegertracing.Configuration.SenderConfiguration;
import io.jaegertracing.internal.JaegerSpan;
import io.jaegertracing.internal.propagation.TextMapCodec;
import io.jaegertracing.spi.Codec;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format.Builtin;
import io.opentracing.propagation.TextMap;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

/**
 * Implementation of {@link TracerConfig} that reports the {@link Span}s to <a
 * href="https://www.jaegertracing.io/">Jaeger</a>
 */
@Restricted(NoExternalUse.class)
public final class JaegerConfig extends TracerConfig {
  private static final SenderConfiguration NULL_SENDER_CONFIG =
      new SenderConfiguration().withAgentPort(1);

  private final String ui;
  private final SenderConfig sender;

  @DataBoundConstructor
  public JaegerConfig(@Nonnull SenderConfig sender, @Nonnull String ui) {
    this.sender = sender;
    this.ui = ui;
  }

  public SenderConfig getSender() {
    return sender;
  }

  private Configuration getConfiguration(String name) {
    return new Configuration(name)
        .withSampler(new SamplerConfiguration().withType("const").withParam(1))
        .withReporter(
            new ReporterConfiguration().withSender(sender.getConfig()).withFlushInterval(2000))
        .withCodec(
            new Configuration.CodecConfiguration().withCodec(Builtin.TEXT_MAP, ENVIRONMENT_CODEC));
  }

  private static final String SPAN_CONTEXT_KEY = "uber_trace_id";
  private static final String BAGGAGE_KEY_PREFIX = "uberctx_";

  private static final Codec<TextMap> ENVIRONMENT_CODEC =
      new TextMapCodec.Builder()
          .withSpanContextKey(SPAN_CONTEXT_KEY)
          .withBaggagePrefix(BAGGAGE_KEY_PREFIX)
          .withUrlEncoding(true)
          .build();

  @Override
  public Tracer getTracerForName(String name) {
    return getConfiguration(name).getTracer();
  }

  @Override
  public Optional<TracerUiLink> getTraceLinkAction(Span span) {
    return Optional.ofNullable(JaegerUIAction.from(ui, span));
  }

  public String getUi() {
    return ui;
  }

  @Restricted(DoNotUse.class)
  public static class ConverterImpl extends BackwardCompatConverter {

    public ConverterImpl(XStream2 xs) {
      super(
          xs,
          field -> {
            if ("jaegerUIUrl".equals(field)) {
              return "ui";
            }
            return null;
          });
    }
  }

  @Extension
  @Symbol("jaeger")
  public static final class DescriptorImpl extends TracerConfig.ConfigDescriptor {

    public DescriptorImpl() {
      super("Jaeger");
    }
  }

  @VisibleForTesting
  abstract static class SenderConfig extends AbstractDescribableImpl<SenderConfig>
      implements ExtensionPoint, Describable<SenderConfig> {

    abstract SenderConfiguration getConfig();
  }

  private abstract static class SenderDescriptor extends Descriptor<SenderConfig> {
    private String name;

    SenderDescriptor(String name) {
      this.name = name;
    }

    @Nonnull
    @Override
    public String getDisplayName() {
      return name;
    }
  }

  public static final class UdpSenderConfig extends SenderConfig {

    private String host;
    private int port;

    @DataBoundConstructor
    public UdpSenderConfig(@Nonnull String host, int port) {
      this.host = host;
      this.port = port;
    }

    @Override
    SenderConfiguration getConfig() {
      return new SenderConfiguration().withAgentHost(host).withAgentPort(port);
    }

    public String getHost() {
      return host;
    }

    public int getPort() {
      return port;
    }

    @Extension
    @Symbol("udp")
    public static final class DescriptorImpl extends SenderDescriptor {

      public DescriptorImpl() {
        super("UDP");
      }

      public FormValidation doCheckPort(@QueryParameter String port) {
        return FormValidation.validatePositiveInteger(port);
      }

      public FormValidation doCheckHost(@QueryParameter String host) {
        return FormValidation.validateRequired(host);
      }
    }
  }

  public static final class HttpSenderConfig extends SenderConfig {

    public String getEndpoint() {
      return endpoint;
    }

    private final String endpoint;

    @DataBoundConstructor
    public HttpSenderConfig(@Nonnull String endpoint) {
      this.endpoint = endpoint;
    }

    @Override
    SenderConfiguration getConfig() {
      return new SenderConfiguration().withEndpoint(endpoint);
    }

    @Extension
    @Symbol("http")
    public static final class DescriptorImpl extends SenderDescriptor {
      public DescriptorImpl() {
        super("HTTP");
      }

      public FormValidation doCheckEndpoint(@QueryParameter String endpoint) {
        return Utils.validateHTTPUrl(endpoint);
      }
    }
  }

  public static final class NullSenderConfig extends SenderConfig {

    @DataBoundConstructor
    public NullSenderConfig() {
      /* for injection */
    }

    @Override
    SenderConfiguration getConfig() {
      return NULL_SENDER_CONFIG;
    }

    @Extension(ordinal = 99)
    @Symbol("null")
    public static final class DescriptorImpl extends SenderDescriptor {
      public DescriptorImpl() {
        super("NOOP");
      }
    }
  }

  private static final class JaegerUIAction implements TracerUiLink {
    private final String url;

    private JaegerUIAction(String url) {
      this.url = url;
    }

    public static @Nullable TracerUiLink from(@Nullable String jaegerUIUrl, Span span) {
      if (jaegerUIUrl == null) {
        return null;
      }
      if (!(span instanceof JaegerSpan)) {
        return null;
      }
      String traceId = ((JaegerSpan) span).context().getTraceId();
      return new JaegerUIAction(jaegerUIUrl + "/trace/" + traceId);
    }

    @Override
    public String getIconFileName() {
      return Utils.getImageUrl("16x16/jaeger.png");
    }

    @Override
    public String getDisplayName() {
      return "Jaeger UI";
    }

    @Override
    public String getUrlName() {
      return url;
    }
  }
}
