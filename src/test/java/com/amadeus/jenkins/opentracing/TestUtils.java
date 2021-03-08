package com.amadeus.jenkins.opentracing;

import java.io.Serializable;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.SerializationUtils;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.jvnet.hudson.test.JenkinsRule;

public class TestUtils {
  private TestUtils() {}

  @Nonnull
  public static <T> T notNull(@Nullable T t) {
    Assertions.assertThat(t).isNotNull();
    return t;
  }

  public static <T extends Serializable> SerializableAssert<T> assertThat(T actual) {
    return new SerializableAssert<>(actual);
  }

  public static class SerializableAssert<T extends Serializable>
      extends AbstractAssert<SerializableAssert<T>, T> {

    private SerializableAssert(T actual) {
      super(actual, SerializableAssert.class);
    }

    @SuppressWarnings("UnusedReturnValue")
    public SerializableAssert<T> satisfiesAfterSerializationRoundTrip(Consumer<T> condition) {
      T roundTripped = SerializationUtils.roundtrip(actual);
      assertThat(roundTripped)
          .as("Roundtripped object from %s to %s", actual, roundTripped)
          .isNotNull()
          .isNotSameAs(actual)
          .satisfies(condition);
      return this;
    }
  }

  // this only works during or after a maven build
  public static void assumePluginManagerInitialized(JenkinsRule j) {
    Assume.assumeTrue("pluginManager is usable", !j.getPluginManager().getPlugins().isEmpty());
  }
}
