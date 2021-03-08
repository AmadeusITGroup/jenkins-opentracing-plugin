package com.amadeus.jenkins.opentracing.test;

import io.opentracing.mock.MockSpan;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

@SuppressWarnings({"UnusedReturnValue", "WeakerAccess"})
public class MockSpanAssert extends AbstractAssert<MockSpanAssert, MockSpan> {
  private MockSpanAssert(MockSpan actual) {
    super(actual, MockSpanAssert.class);
  }

  public static MockSpanAssert assertThat(MockSpan actual) {
    return new MockSpanAssert(actual);
  }

  private void assertSameTrace(MockSpan other) {
    Assertions.assertThat(actual.context().traceId()).isEqualTo(other.context().traceId());
  }

  public MockSpanAssert isDirectChildOf(MockSpan parent) {
    isNotNull();
    assertThat(parent).isNotNull();

    assertSameTrace(parent);
    Assertions.assertThat(actual.parentId())
        .as("id of parent matches")
        .isEqualTo(parent.context().spanId());

    return this;
  }

  public MockSpanAssert isTemporallyEnclosedBy(MockSpan outer) {

    Assertions.assertThat(actual.startMicros()).isGreaterThan(outer.startMicros());
    // sometimes we finish multiple at once, this may happen in the same millisecond.
    // (Mocktracer only has millisecond resolution)
    Assertions.assertThat(actual.finishMicros()).isLessThanOrEqualTo(outer.finishMicros());

    return this;
  }

  public MockSpanAssert isTemporallyEnclosedDirectChildOf(MockSpan parent) {
    return isDirectChildOf(parent).isTemporallyEnclosedBy(parent);
  }

  public MockSpanAssert isSibling(MockSpan... siblings) {
    for (MockSpan sibling : siblings) {
      assertSameTrace(sibling);
      isNotEqualTo(sibling);
      Assertions.assertThat(actual.parentId()).isEqualTo(sibling.parentId());
    }

    return this;
  }

  public MockSpanAssert isTemporallyAfter(MockSpan other) {
    Assertions.assertThat(actual.startMicros()).isGreaterThanOrEqualTo(other.finishMicros());

    return this;
  }

  public MockSpanAssert hasNoTags() {
    Assertions.assertThat(actual.tags()).isEmpty();
    return this;
  }

  public MockSpanAssert hasOnlyTags(String... names) {
    Assertions.assertThat(actual.tags()).containsOnlyKeys(names);
    return this;
  }

  public MockSpanAssert hasOnlyUncommonTags(String... names) {
    Assertions.assertThat(uncommonTags()).containsOnlyKeys(names);
    return this;
  }

  public MockSpanAssert hasNoUncommonTags() {
    Assertions.assertThat(uncommonTags()).isEmpty();
    return this;
  }

  public MockSpanAssert hasTag(String name) {
    Assertions.assertThat(actual.tags()).containsKey(name);
    return this;
  }

  public MockSpanAssert hasTag(String name, Object value) {
    Assertions.assertThat(actual.tags()).containsEntry(name, value);
    return this;
  }

  private Map<String, Object> uncommonTags() {
    List<String> COMMON_TAGS = Arrays.asList("jenkins.url", "jenkins.rooturl", "step.functionName");

    return actual.tags().entrySet().stream()
        .filter(e -> !COMMON_TAGS.contains(e.getKey()))
        .filter(e -> !e.getKey().startsWith("step.arguments."))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }
}
