package com.amadeus.jenkins.opentracing.workflow;

import com.amadeus.jenkins.opentracing.Utils;
import hudson.model.TaskListener;
import io.opentracing.Span;
import java.util.Map;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/** DSL object for easy access to tags. */
public final class WorkflowSpanTags {
  private final Span span;
  private final TaskListener listener;

  public WorkflowSpanTags(Span span, @Nullable TaskListener listener) {
    this.span = span;
    this.listener = listener;
  }

  /** See {@link Span#setTag(String, String)} */
  @Whitelisted
  public void putAt(String key, String value) {
    span.setTag(key, value);
  }

  /** See {@link Span#setTag(String, Number)} */
  @Whitelisted
  public void putAt(String key, Number value) {
    span.setTag(key, value);
  }

  /** See {@link Span#setTag(String, boolean)} */
  @Whitelisted
  public void putAt(String key, boolean value) {
    span.setTag(key, value);
  }

  @Whitelisted
  public WorkflowSpanTags plus(Map<String, Object> tags) {
    tags.forEach(
        (k, v) -> {
          if (!Utils.setTag(span, k, v) && listener != null) {
            listener
                .getLogger()
                .format("Could not add tag %s of unsupported type %s", k, v.getClass());
          }
        });
    return this;
  }
}
