package com.amadeus.jenkins.opentracing.workflow;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Simple key=value type to work with Jenkins' databinding that does not work for normal {@link
 * Map}s.
 */
@Restricted(NoExternalUse.class)
public final class Tag {
  private final String name;
  private final String value;

  @DataBoundConstructor
  public Tag(@Nonnull String name, @Nonnull String value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return name + "=" + value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Tag tag = (Tag) o;
    return Objects.equals(name, tag.name) && Objects.equals(value, tag.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, value);
  }

  static List<Tag> fromMap(Map<String, String> tags) {
    return tags.entrySet().stream()
        .map(e -> new Tag(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  static Map<String, String> toMap(@Nullable List<Tag> tags) {
    if (tags == null) {
      return Collections.emptyMap();
    }
    return tags.stream().collect(Collectors.toMap(Tag::getName, Tag::getValue));
  }
}
