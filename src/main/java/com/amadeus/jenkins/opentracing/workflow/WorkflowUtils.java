package com.amadeus.jenkins.opentracing.workflow;

import io.opentracing.Span;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Map;
import javax.annotation.Nullable;

final class WorkflowUtils {
  private WorkflowUtils() {}

  public static long microseconds(TemporalAccessor timestamp) {
    return timestamp.getLong(ChronoField.INSTANT_SECONDS) * 1_000_000
        + timestamp.get(ChronoField.MICRO_OF_SECOND);
  }

  static void addInformation(Span span, SpanInformation info) {
    String operationName = info.getOperationName();
    if (operationName != null) {
      span.setOperationName(operationName);
    }
    Map<String, String> tags = info.getTags();
    if (tags != null) {
      tags.forEach(span::setTag);
    }
  }

  public interface SpanInformation {
    @Nullable
    Map<String, String> getTags();

    @Nullable
    String getOperationName();
  }
}
