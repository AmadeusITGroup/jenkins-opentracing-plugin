package com.amadeus.jenkins.opentracing;

import com.google.common.annotations.VisibleForTesting;
import hudson.Extension;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Centralized storage to hold all references to {@link io.opentracing.Span}s. This centralized
 * access * ensure that all references to Jenkins internal objects are {@link WeakReference}s or
 * entries in {@link WeakHashMap}s. Thus the plugin will not leak these objects. * Allows the
 * configuration system to flush all in-flight {@link io.opentracing.Span}s on reconfiguration,
 * ensuring that the newly configured tracer will not be asked to handle spans from the previous
 * one. (The two tracers could belong to completely different tracing systems)
 */
@Extension
@Restricted(NoExternalUse.class)
public final class SpanStorage {
  private final Map<SimpleImmutableEntry<Object, String>, Reference<Object>> references =
      Collections.synchronizedMap(new WeakHashMap<>());
  private final Map<SimpleImmutableEntry<Object, String>, Map<Object, Object>> caches =
      Collections.synchronizedMap(new HashMap<>());
  private Supplier<Map<?, ?>> cacheSupplier =
      () -> Collections.synchronizedMap(new WeakHashMap<>());

  @SuppressWarnings("unchecked")
  public <T> Reference<T> getReference(Object requester, String discriminator, T value) {
    Reference<T> ref = new WeakReference<>(value);
    references.put(new SimpleImmutableEntry(requester, discriminator), (Reference<Object>) ref);
    return ref;
  }

  public <K, V> Map<K, V> getCache(Class<?> requester) {
    return getCache(requester, null);
  }

  @SuppressWarnings("unchecked")
  public <K, V> Map<K, V> getCache(Object requester, @Nullable String discriminator) {
    return (Map<K, V>)
        caches.computeIfAbsent(
            new SimpleImmutableEntry(requester, discriminator),
            x -> (Map<Object, Object>) cacheSupplier.get());
  }

  @VisibleForTesting
  void setCacheSupplier(Supplier<Map<?, ?>> supplier) {
    this.cacheSupplier = supplier;
  }

  public void flush() {
    caches.values().forEach(Map::clear);
    references.clear();
  }

  public long size() {
    return caches.values().stream().mapToLong(Map::size).sum() + references.size();
  }
}
