package com.amadeus.jenkins.opentracing;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Run;
import io.opentracing.Span;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.flow.FlowExecutionListener;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link FlowExecutionListener} that attaches {@link OTGraphListener} to pipeline jobs. Also keeps
 * track of attached listeners and provides access to this information.
 */
@Extension
@Restricted(NoExternalUse.class)
public final class OTFlowExecutionListener extends FlowExecutionListener {
  private static final Logger logger = LoggerFactory.getLogger(OTFlowExecutionListener.class);

  private final Map<FlowExecution, OTGraphListener> graphListeners =
      ExtensionList.lookupSingleton(SpanStorage.class).getCache(OTFlowExecutionListener.class);
  private final OTRunListener runListener = ExtensionList.lookupSingleton(OTRunListener.class);

  @Override
  public void onRunning(@Nonnull FlowExecution execution) {
    OTGraphListener listener = getListener(execution);
    if (listener != null) {
      execution.addListener(listener);
    }
  }

  @Override
  public void onResumed(@Nonnull FlowExecution execution) {
    /* not relevant for us (so far) */
  }

  @Override
  public void onCompleted(@Nonnull FlowExecution execution) {
    graphListeners.remove(execution);
  }

  public @Nullable OTGraphListener getListener(FlowExecution execution) {
    return graphListeners.computeIfAbsent(
        execution,
        e -> {
          Run run = getRun(e);
          if (run == null) {
            return null;
          }
          Span span = runListener.getSpan(run);
          if (span == null) {
            return null;
          }
          return new OTGraphListener(span);
        });
  }

  private static @Nullable Run getRun(FlowExecution execution) {
    try {
      return (Run) execution.getOwner().getExecutable();
    } catch (IOException | ClassCastException e) {
      logger.error("Unable to find run for FlowExecution {}", execution, e);
      return null;
    }
  }
}
