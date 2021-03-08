package com.amadeus.jenkins.opentracing;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.config.TracerUiLink;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * {@link RunListener} keeping track of all executed {@link Run}s and connects them to OpenTracing.
 * Also provides access to these mappings.
 */
@Extension
@Restricted(NoExternalUse.class)
public final class OTRunListener extends RunListener<Run> {
  private final Tracer tracer;
  private final Map<Run, Span> runSpans =
      ExtensionList.lookupSingleton(SpanStorage.class).getCache(OTRunListener.class);
  private final OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);

  public OTRunListener() {
    tracer = config.getTracerForName("Jenkins Jobs");
  }

  @Override
  public void onStarted(Run run, TaskListener listener) {
    Span parent = OTQueueListener.getInstance().popQueueSpan(run.getQueueId());

    SpanBuilder builder = tracer.buildSpan(String.format("Job %s", run.getDisplayName()));
    builder.ignoreActiveSpan();

    if (parent != null) {
      builder.asChildOf(parent);
    }

    builder.withTag("jenkins.job", run.getDisplayName());
    builder.withTag("jenkins.build.number", run.number);

    Span span = builder.start();
    Utils.addUrlTag(span, run);
    runSpans.put(run, span);

    Optional<TracerUiLink> uiLink = config.getLink(span);
    uiLink.ifPresent(run::addAction);
  }

  public @Nullable Span getSpan(Run run) {
    return runSpans.get(run);
  }

  @Override
  public void onCompleted(Run run, @Nonnull TaskListener listener) {
    run.getUrl();
    Span span = runSpans.get(run);
    if (span != null) {
      Result result = run.getResult();
      if (result != null) {
        span.setTag("jenkins.result", result.toString());
        if (!result.equals(Result.SUCCESS)) {
          Utils.setError(span, null);
        }
      }
      span.finish();
    }
  }
}
