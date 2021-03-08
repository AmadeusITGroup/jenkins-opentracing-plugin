package com.amadeus.jenkins.opentracing;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;
import hudson.model.Queue;
import hudson.model.Queue.BlockedItem;
import hudson.model.Queue.BuildableItem;
import hudson.model.Queue.Item;
import hudson.model.Queue.LeftItem;
import hudson.model.Queue.Task;
import hudson.model.Queue.WaitingItem;
import hudson.model.queue.QueueListener;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* FIXME implement all possible queue transitions */

/**
 * {@link QueueListener} connecting the Jenkis queuing subsystem with OpenTracing. Also provides
 * access to the mappings.
 */
@Extension
@Restricted(NoExternalUse.class)
public final class OTQueueListener extends QueueListener {
  private static Logger logger = LoggerFactory.getLogger(OTQueueListener.class);

  private final SpanStorage spanStorage = ExtensionList.lookupSingleton(SpanStorage.class);
  private final Map<Long, Span> spanInQueue = getCache("spanInQueue");
  private final Map<WaitingItem, Span> spanInWaiting = getCache("spanInWaiting");
  private final Map<BlockedItem, Span> spanInBlocked = getCache("spanInBlocked");
  private final Map<BuildableItem, Span> spanInBuildable = getCache("spanInBuildable");
  private final Map<LeftItem, Span> spanLeft = getCache("spanLeft");

  private Tracer tracer;
  private final OTFlowExecutionListener flowExecutionListener;

  public OTQueueListener() {
    tracer = ExtensionList.lookupSingleton(OTConfig.class).getTracerForName("Jenkins Queue");
    flowExecutionListener = ExtensionList.lookupSingleton(OTFlowExecutionListener.class);
  }

  static OTQueueListener getInstance() {
    return QueueListener.all().getInstance(OTQueueListener.class);
  }

  private Span enterQueue(Item i) {
    logger.debug(
        "EnterQueue {} {} {} {} {} {}",
        i.getId(),
        i.task.getFullDisplayName(),
        i.task.getUrl(),
        i.task.getClass(),
        i.task,
        i.task.getOwnerTask());

    return spanInQueue.computeIfAbsent(
        i.getId(),
        task -> {
          SpanBuilder builder = tracer.buildSpan("Queue " + i.task.getFullDisplayName());
          String username = getUsername(i);

          Span parent = getParentFromWorkflowNodeAllocation(i.task).orElse(null);
          builder.asChildOf(parent);
          Utils.addUrlTag(builder, i);

          if (username != null) {
            builder.withTag("username", username);
          }
          builder.ignoreActiveSpan();
          return builder.start();
        });
  }

  private Optional<Span> getParentFromWorkflowNodeAllocation(Task task) {
    return getNode(task)
        .flatMap(
            node -> {
              OTGraphListener graphListener =
                  flowExecutionListener.getListener(node.getExecution());
              return Optional.ofNullable(graphListener).flatMap(gl -> gl.getSpan(node));
            });
  }

  // Handles org.jenkinsci.plugins.workflow.support.steps;.ExecutorStepExecution$PlaceHolderTask
  private static Optional<FlowNode> getNode(Task task) {
    try {
      Method getNodeMethod = task.getClass().getMethod("getNode");
      if (!(FlowNode.class.equals(getNodeMethod.getReturnType()))) {
        logger.warn(
            "Found 'getNode' method with unexpected return type: {}",
            getNodeMethod.getReturnType());
        return Optional.empty();
      }
      if (getNodeMethod.getParameterCount() != 0) {
        logger.warn(
            "Found 'getNode' method with unexpected parameters: {}",
            getNodeMethod.getParameterCount());
        return Optional.empty();
      }
      return Optional.ofNullable((FlowNode) getNodeMethod.invoke(task));
    } catch (NoSuchMethodException e) {
      logger.trace("Task {} has not 'getNode' method, ignoring", task, e);
    } catch (IllegalAccessException | InvocationTargetException e) {
      logger.debug("Could not execute 'getNode' on {}: {}", task.getClass(), task, e);
    }
    return Optional.empty();
  }

  private static @Nullable String getUsername(Item i) {
    for (Cause cause : i.getCauses()) {
      if (cause instanceof UserIdCause) {
        return ((UserIdCause) cause).getUserName();
      }
    }
    return null;
  }

  @Override
  public void onEnterWaiting(WaitingItem wi) {
    logger.debug("Enter Waiting {} {}", wi, wi.getAllActions());

    Span parent = enterQueue(wi);

    SpanBuilder builder = tracer.buildSpan("Waiting");
    builder.ignoreActiveSpan();
    builder.asChildOf(parent);
    builder.withTag("reason", wi.getCauseOfBlockage().getShortDescription());
    Utils.addUrlTag(builder, wi);

    Span span = builder.start();
    spanInWaiting.put(wi, span);
  }

  @Override
  public void onLeaveWaiting(WaitingItem wi) {
    logger.debug("Leave Waiting {} {}", wi, wi.getAllActions());
    Span span = spanInWaiting.remove(wi);
    if (span == null) {
      return;
    }
    span.finish();
  }

  @Override
  public void onEnterBlocked(Queue.BlockedItem bi) {
    logger.debug("Enter Blocked {} {}", bi, bi.getAllActions());

    Span parent = enterQueue(bi);

    SpanBuilder builder = tracer.buildSpan("Blocked");
    builder.ignoreActiveSpan();
    builder.asChildOf(parent);
    Utils.addUrlTag(builder, bi);

    Span span = builder.start();
    spanInBlocked.put(bi, span);
  }

  @Override
  public void onLeaveBlocked(BlockedItem bi) {
    logger.debug("Leave Blocked {} {}", bi, bi.getAllActions());
    Span span = spanInBlocked.remove(bi);
    if (span == null) {
      return;
    }
    span.finish();
  }

  @Override
  public void onEnterBuildable(BuildableItem bi) {
    logger.debug("Enter Buildable {} {}", bi, bi.getAllActions());

    Span parent = enterQueue(bi);

    SpanBuilder builder = tracer.buildSpan("Buildable");
    builder.ignoreActiveSpan();
    builder.asChildOf(parent);
    Utils.addUrlTag(builder, bi);

    Span span = builder.start();
    spanInBuildable.put(bi, span);
  }

  @Override
  public void onLeaveBuildable(BuildableItem bi) {
    logger.debug("Leave Buildable {} {}", bi, bi.getAllActions());
    Span span = spanInBuildable.remove(bi);
    if (span == null) {
      return;
    }
    span.finish();
  }

  public Optional<Span> spanForLeftItem(LeftItem item) {
    return Optional.ofNullable(spanLeft.get(item));
  }

  @Override
  public void onLeft(LeftItem li) {
    logger.debug("Left {} {}", li, li.getAllActions());
    Span span = spanInQueue.get(li.getId());
    if (span == null) {
      return;
    }
    span.setTag("cancelled", li.isCancelled());
    span.finish();

    spanLeft.put(li, span);
  }

  public @Nullable Span popQueueSpan(long queueId) {
    Span span = spanInQueue.remove(queueId);
    logger.debug("Looking for parent for queueId {}: {}", queueId, span);
    return span;
  }

  private <K> Map<K, Span> getCache(String name) {
    return spanStorage.getCache(OTQueueListener.class, name);
  }
}
