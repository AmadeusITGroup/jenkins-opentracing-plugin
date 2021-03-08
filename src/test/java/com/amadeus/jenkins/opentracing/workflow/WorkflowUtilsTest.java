package com.amadeus.jenkins.opentracing.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;

public class WorkflowUtilsTest {

  @Test
  public void testMicroseconds() {
    Instant i = Instant.ofEpochSecond(111111, 222333444);
    assertThat(WorkflowUtils.microseconds(i)).isEqualTo(111111222333L);
  }

  @Test
  public void testMicrosecondsDatetime() {
    Instant i = Instant.ofEpochSecond(111111, 222333444);
    ZonedDateTime x = ZonedDateTime.ofInstant(i, ZoneId.of("UTC"));
    assertThat(WorkflowUtils.microseconds(x)).isEqualTo(111111222333L);
  }
}
