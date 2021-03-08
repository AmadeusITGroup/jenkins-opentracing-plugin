package com.amadeus.jenkins.opentracing.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.TestUtils;
import java.util.Collections;
import java.util.Map;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Test;

@SuppressWarnings("ConstantConditions")
public class TraceStepExecutionsTest {
  private static final StepContext context = null;
  private static final Map<String, String> tags = Collections.singletonMap("foo", "bar");
  private static final String operationName = "someOperation";

  @Test
  public void testAtomStepExecutionSerialization() {
    TestUtils.assertThat(new TraceStepAtomExecution(context, tags))
        .satisfiesAfterSerializationRoundTrip(
            after -> {
              assertThat(after.getTags()).isNotSameAs(tags).isEqualTo(tags);
            });
  }

  @Test
  public void testBodyStepExecutionSerialization() {
    TestUtils.assertThat(new TraceStepBodyExecution(context, operationName, tags))
        .satisfiesAfterSerializationRoundTrip(
            after -> {
              assertThat(after.getTags()).isNotSameAs(tags).isEqualTo(tags);
              assertThat(after.getOperationName()).isEqualTo(operationName);
            });
  }
}
