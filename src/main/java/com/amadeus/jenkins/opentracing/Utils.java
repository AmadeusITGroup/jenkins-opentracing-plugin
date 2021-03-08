package com.amadeus.jenkins.opentracing;

import hudson.ExtensionList;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.util.FormValidation;
import io.opentracing.Span;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.tag.Tags;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Optional;
import javax.annotation.Nullable;
import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Restricted(NoExternalUse.class)
public final class Utils {
  private Utils() {}

  private static final Logger logger = LoggerFactory.getLogger(Utils.class);
  public static final String PLUGIN_SHORT_NAME = "opentracing";

  public static FormValidation validateHTTPUrl(String value) {
    FormValidation passedRequired = FormValidation.validateRequired(value);
    if (!passedRequired.kind.equals(FormValidation.Kind.OK)) {
      return passedRequired;
    }

    try {
      URL parsed = new URL(value);
      if ((!"http".equals(parsed.getProtocol()) && (!"https".equals(parsed.getProtocol())))) {
        return FormValidation.error("URL has to be http/https");
      }
    } catch (MalformedURLException e) {
      return FormValidation.error("Invalid URL passed: %s", e.getMessage());
    }
    return FormValidation.ok();
  }

  public static String getImageUrl(String name) {
    return "/plugin/" + PLUGIN_SHORT_NAME + "/images/" + name;
  }

  public static void addUrlTag(Span span, Item item) {
    addUrlTag(span, item.getUrl());
  }

  public static void addUrlTag(Span span, Run run) {
    addUrlTag(span, run.getUrl());
  }

  public static void addUrlTag(Span span, FlowNode node) {
    try {
      addUrlTag(span, node.getUrl());
    } catch (IOException e) {
      logger.trace("Could not get url of node {}", node, e);
    }
  }

  public static void addUrlTag(SpanBuilder spanBuilder, Queue.Item item) {
    addUrlTag(spanBuilder, item.getUrl());
  }

  public static void setError(Span span, @Nullable String message) {
    Tags.ERROR.set(span, true);
    if (message != null) {
      span.setTag("error.message", message);
    }
  }

  public static void addRootUrlTag(SpanBuilder spanBuilder) {
    spanBuilder.withTag("jenkins.rooturl", Jenkins.get().getRootUrl());
  }

  private static void addUrlTag(Span span, String relativeUrl) {
    getAbsoluteUrl(relativeUrl).ifPresent(url -> span.setTag("jenkins.url", url));
  }

  private static void addUrlTag(SpanBuilder spanBuilder, String relativeUrl) {
    getAbsoluteUrl(relativeUrl).ifPresent(url -> spanBuilder.withTag("jenkins.url", url));
  }

  private static Optional<String> getAbsoluteUrl(String relativeUrl) {
    JenkinsLocationConfiguration locationConfig;
    try {
      locationConfig = JenkinsLocationConfiguration.get();
    } catch (IllegalStateException e) {
      /* baseurl not configured */
      return Optional.empty();
    }
    String baseUrl = locationConfig.getUrl();
    if (baseUrl == null) {
      return Optional.empty();
    }
    return Optional.of(baseUrl + relativeUrl);
  }

  public static Optional<FlowNode> getEnclosing(FlowNode node) {
    Iterator<BlockStartNode> iter = node.iterateEnclosingBlocks().iterator();
    if (iter.hasNext()) {
      return Optional.of(iter.next());
    }
    return Optional.empty();
  }

  public static boolean setTag(Span span, String name, Object value) {
    if (value instanceof String) {
      span.setTag(name, (String) value);
    } else if (value instanceof Number) {
      span.setTag(name, (Number) value);
    } else if (value instanceof Boolean) {
      span.setTag(name, (Boolean) value);
    } else {
      return false;
    }
    return true;
  }

  // if for some reason the no TimingAction is present we fall back to the current time
  // this may lead to slightly wrong timing but this is preferable to errors or wildly wrong data
  public static void startSpanWithFlowNodeTiming(SpanBuilder spanBuilder, FlowNode node) {
    long startMillis = TimingAction.getStartTime(node);
    if (startMillis != 0) {
      spanBuilder.withStartTimestamp(startMillis * 1000);
    } else {
      logger.debug(
          "FlowNode {} has no start timing information, falling back to current timestamp", node);
    }
  }

  public static void finishSpanWithFlowNodeTiming(Span span, FlowNode node) {
    long finishMillis = TimingAction.getStartTime(node);
    if (finishMillis != 0) {
      span.finish(finishMillis * 1000);
    } else {
      logger.debug(
          "FlowNode {} has no end timing information, falling back to current timestamp", node);
      span.finish();
    }
  }

  public static @Nullable <U> U tryLookupSingleton(Class<U> type) {
    ExtensionList<U> all = ExtensionList.lookup(type);
    if (all.isEmpty()) {
      return null;
    }
    if (all.size() == 1) {
      return all.get(0);
    }
    throw new IllegalStateException("Found " + all.size() + " of " + type.getName());
  }
}
