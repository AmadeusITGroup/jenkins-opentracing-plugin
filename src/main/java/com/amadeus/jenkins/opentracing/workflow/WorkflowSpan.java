package com.amadeus.jenkins.opentracing.workflow;

import static com.amadeus.jenkins.opentracing.workflow.WorkflowUtils.microseconds;

import hudson.model.TaskListener;
import io.opentracing.Span;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

/**
 * This class is exposed in the Pipeline DSL to Pipeline authors The API resembles the one of normal
 * OpenTracing {@link Span}s. The wrapped delegate is intentionally not exposed as it would require
 * to whitelist these methods on the implementations.
 */
public final class WorkflowSpan {
  private final Span delegate;
  private final TaskListener listener;

  WorkflowSpan(Span delegate, @Nullable TaskListener listener) {
    Objects.requireNonNull(delegate);

    this.delegate = delegate;
    this.listener = listener;
  }

  /** See {@link Span#setTag(String, String)} */
  @Whitelisted
  public WorkflowSpan setTag(String key, String value) {
    delegate.setTag(key, value);
    return this;
  }

  /** See {@link Span#setTag(String, Number)} */
  @Whitelisted
  public WorkflowSpan setTag(String key, Number value) {
    delegate.setTag(key, value);
    return this;
  }

  /** See {@link Span#setTag(String, boolean)} */
  @Whitelisted
  public WorkflowSpan setTag(String key, boolean value) {
    delegate.setTag(key, value);
    return this;
  }

  /** See {@link Span#log(Map)} */
  @Whitelisted
  public WorkflowSpan log(Map<String, ?> fields) {
    delegate.log(fields);
    return this;
  }

  /** See {@link Span#log(long, Map)} */
  @Whitelisted
  public WorkflowSpan log(long timestampMicroseconds, Map<String, ?> fields) {
    delegate.log(timestampMicroseconds, fields);
    return this;
  }

  /** See {@link Span#log(long, Map)} */
  @Whitelisted
  public WorkflowSpan log(TemporalAccessor timestamp, Map<String, ?> fields) {
    delegate.log(microseconds(timestamp), fields);
    return this;
  }

  /** See {@link Span#log(String)} */
  @Whitelisted
  public WorkflowSpan log(String event) {
    delegate.log(event);
    return this;
  }

  /** See {@link Span#log(long, String)} */
  @Whitelisted
  public WorkflowSpan log(long timestampMicroseconds, String event) {
    delegate.log(timestampMicroseconds, event);
    return this;
  }

  /** See {@link Span#log(long, String)} */
  @Whitelisted
  public WorkflowSpan log(TemporalAccessor timestamp, String event) {
    log(microseconds(timestamp), event);
    return this;
  }

  @Whitelisted
  public WorkflowSpanTags getTags() {
    return new WorkflowSpanTags(this.delegate, listener);
  }
}
