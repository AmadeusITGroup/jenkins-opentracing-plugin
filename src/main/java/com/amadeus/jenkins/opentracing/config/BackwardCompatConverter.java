package com.amadeus.jenkins.opentracing.config;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.SerializableConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import hudson.diagnosis.OldDataMonitor;
import hudson.model.Saveable;
import hudson.util.RobustReflectionConverter;
import hudson.util.XStream2;
import java.util.function.Function;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

@Restricted(NoExternalUse.class)
public class BackwardCompatConverter implements Converter {
  private final BackwardCompatMapper mapper;
  private final Converter reflectionConverter;
  private final Converter serializableConverter;

  public BackwardCompatConverter(XStream2 xs, NameMapper fieldMapper) {
    mapper = new BackwardCompatMapper(xs.getMapper(), fieldMapper);
    reflectionConverter = new RobustReflectionConverter(mapper, xs.getReflectionProvider());
    serializableConverter =
        new SerializableConverter(mapper, xs.getReflectionProvider(), xs.getClassLoaderReference());
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    reflectionConverter.marshal(source, writer, context);
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    Object o;
    boolean oldData = false;
    if (reader.getAttribute("serialization") != null) {
      o = serializableConverter.unmarshal(reader, context);
      oldData = true;
    } else {
      o = reflectionConverter.unmarshal(reader, context);
    }
    if (mapper.hasMapped) {
      oldData = true;
    }
    if (oldData && o instanceof Saveable) {
      OldDataMonitor.report((Saveable) o, "unknown");
    }
    return o;
  }

  @Override
  public boolean canConvert(Class type) {
    throw new IllegalStateException("This method should not be called");
  }

  @FunctionalInterface
  public interface NameMapper extends Function<String, String> {}

  private static class BackwardCompatMapper extends MapperWrapper {
    private final NameMapper fieldMapper;
    private boolean hasMapped = false;

    public BackwardCompatMapper(Mapper wrapped, NameMapper fieldMapper) {
      super(wrapped);
      this.fieldMapper = fieldMapper;
    }

    @Override
    public String realMember(Class type, String serialized) {
      String mapped = fieldMapper.apply(serialized);
      if (mapped != null) {
        hasMapped = true;
        return mapped;
      }
      return super.realMember(type, serialized);
    }
  }
}
