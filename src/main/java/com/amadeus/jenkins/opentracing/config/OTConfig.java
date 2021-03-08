package com.amadeus.jenkins.opentracing.config;

import com.amadeus.jenkins.opentracing.SpanStorage;
import com.amadeus.jenkins.opentracing.Utils;
import com.amadeus.jenkins.opentracing.config.impl.NullConfig;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.init.Terminator;
import hudson.util.XStream2;
import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import jenkins.YesNoMaybe;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Global configuration (UI) for the plugin. Takes care of selecting the correct backend and
 * invalidating the {@link SpanStorage} on settings changes.
 */
@Extension(dynamicLoadable = YesNoMaybe.NO)
@Restricted(NoExternalUse.class)
@Symbol(Utils.PLUGIN_SHORT_NAME)
public final class OTConfig extends GlobalConfiguration {

  private static final NullConfig NULL_CONFIG = new NullConfig();
  @XStreamOmitField private Map<String, DelegatingTracer> tracers = makeMap();

  private TracerConfig tracer;

  public TracerConfig getTracer() {
    if (tracer == null) {
      return NULL_CONFIG;
    }
    return tracer;
  }

  @DataBoundSetter
  public void setTracer(@Nullable TracerConfig tracer) {
    this.tracer = tracer;
    reload();
  }

  public OTConfig() {
    super();
    load();
  }

  @Override
  public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
    super.configure(req, json);
    save();
    return true;
  }

  private void reload() {
    SpanStorage spanStorage = ExtensionList.lookupSingleton(SpanStorage.class);
    spanStorage.flush();
    tracers.forEach((s, t) -> t.setTracerFactory(n -> getTracer().getTracerForName(n)));
    spanStorage.flush();
  }

  public Tracer getTracerForName(String serviceName) {
    return tracers.computeIfAbsent(
        serviceName, s -> new DelegatingTracer(serviceName, n -> getTracer().getTracerForName(n)));
  }

  public Optional<TracerUiLink> getLink(Span span) {
    return getTracer().getTraceLinkAction(span);
  }

  @Restricted(DoNotUse.class)
  @Terminator
  public void closeTracer() {
    for (Tracer t : tracers.values()) {
      t.close();
    }
  }

  private static <K, V> Map<K, V> makeMap() {
    return Collections.synchronizedMap(new HashMap<>());
  }

  private static final class DelegatingTracer implements Tracer {
    private final String serviceName;
    private Tracer delegate;

    private DelegatingTracer(String serviceName, Function<String, Tracer> tracerFactory) {
      this.serviceName = serviceName;
      setTracerFactory(tracerFactory);
    }

    private synchronized void setTracerFactory(Function<String, Tracer> tracerFactory) {
      /* FIXME https://github.com/opentracing/opentracing-java/issues/346
      if (delegate != null) {
        delegate.close();
      }
      */
      delegate = tracerFactory.apply(serviceName);
    }

    @Override
    public ScopeManager scopeManager() {
      return delegate.scopeManager();
    }

    @Override
    public Span activeSpan() {
      return delegate.activeSpan();
    }

    @Override
    public Scope activateSpan(Span span) {
      return delegate.activateSpan(span);
    }

    @Override
    public SpanBuilder buildSpan(String operationName) {
      SpanBuilder builder = delegate.buildSpan(operationName);
      Utils.addRootUrlTag(builder);
      return builder;
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier) {
      delegate.inject(spanContext, format, carrier);
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier) {
      return delegate.extract(format, carrier);
    }

    @Override
    public void close() {
      delegate.close();
    }

    @Override
    public String toString() {
      return "DelegatingTracer{" + ", delegate=" + delegate + '}';
    }
  }

  @Restricted(DoNotUse.class)
  public static class ConverterImpl extends BackwardCompatConverter {

    public ConverterImpl(XStream2 xs) {
      super(
          xs,
          field -> {
            if ("tracerConfig".equals(field)) {
              return "tracer";
            }
            return null;
          });
    }
  }
}
