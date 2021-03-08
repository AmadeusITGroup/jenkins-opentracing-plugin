package com.amadeus.jenkins.opentracing;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.workflow.TraceStep.DescriptorImpl;
import hudson.ExtensionList;
import hudson.model.Queue;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import java.lang.ref.Reference;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.actions.ArgumentsAction;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.QueueItemAction;
import org.jenkinsci.plugins.workflow.actions.StageAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.StepNode;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * {@link GraphListener} connecting the Pipeline graph to OpenTracing. Also provides access to the
 * connections.
 */
@Restricted(NoExternalUse.class)
public final class OTGraphListener implements GraphListener, GraphListener.Synchronous {
  private final Tracer tracer;
  private final Map<FlowNode, State> states;
  private final Reference<Span> flowParentSpan;

  OTGraphListener(Span flowParentSpan) {
    SpanStorage storage = ExtensionList.lookupSingleton(SpanStorage.class);
    tracer = ExtensionList.lookupSingleton(OTConfig.class).getTracerForName("Jenkins Pipeline");
    states = storage.getCache(this, "states");
    this.flowParentSpan = storage.getReference(this, "flowParentSpan", flowParentSpan);
  }

  @Override
  public void onNewHead(FlowNode node) {
    // if block end, stop its span
    if (is(node, BlockEndNode.class)) {
      // remove/get
      // we receive two end nodes per start node
      State startState = states.get(((BlockEndNode) node).getStartNode());
      // FIXME delete
      finishPreviousNodes(node);
      if (startState != null) {
        State activeChildState = states.get(startState.getActiveChild());
        if (activeChildState != null) {
          activeChildState.finish(node);
        }

        // The actions are only added when the step is started,
        // not when it is created, which is *after* we can run.
        // therefore only set it at the end of the block
        completeStartNodeInformation(startState.getSpan(), (BlockStartNode) startState.getNode());

        startState.finish(node);
      }
      return;
    }

    if (!is(node, AtomNode.class, BlockStartNode.class)) {
      return;
    }

    onNewStartOrAtomNode(node);
  }

  private void finishPreviousNodes(FlowNode node) {
    FlowNode previous;
    Optional<State> parent = enclosingState(node);
    if (!parent.isPresent()) {
      return;
    }
    previous = parent.get().getActiveChild();
    parent.get().setActiveChild(node);

    // if previous atom, stop its span
    State previousState = states.get(previous);
    if (previousState != null && previousState.getNode() instanceof AtomNode) {
      Utils.finishSpanWithFlowNodeTiming(previousState.getSpan(), node);
    }
  }

  private static boolean is(Object o, Class... classes) {
    for (Class clazz : classes) {
      if (clazz.isInstance(o)) {
        return true;
      }
    }
    return false;
  }

  private Optional<Queue.LeftItem> getQueueItem(FlowNode node) {
    for (FlowNode parent : node.iterateEnclosingBlocks()) {
      QueueItemAction queueItemAction = parent.getAction(QueueItemAction.class);
      if (queueItemAction != null) {
        return Optional.ofNullable((Queue.LeftItem) queueItemAction.itemInQueue());
      }
    }
    return Optional.empty();
  }

  public @Nullable Span onNewStartOrAtomNode(FlowNode node) {
    if (node instanceof AtomNode && isCustomTraceStep(node)) {
      return null;
    }
    State existingState = states.get(node);
    if (existingState != null) {
      return existingState.getSpan();
    }

    final Span parentSpan;
    finishPreviousNodes(node);

    Optional<Queue.LeftItem> queueItem = getQueueItem(node);

    if (queueItem.isPresent()) {
      parentSpan = OTQueueListener.getInstance().spanForLeftItem(queueItem.get()).orElse(null);
    } else {
      Optional<State> enclosingState = enclosingState(node);

      if (enclosingState.isPresent()) {
        parentSpan = enclosingState.get().getSpan();
      } else {
        parentSpan = flowParentSpan.get();
      }
    }

    String operationName;
    if (node instanceof AtomNode) {
      operationName = node.getDisplayFunctionName();
    } else {
      operationName = node.getDisplayName() + " " + node.getDisplayFunctionName();
    }

    SpanBuilder spanBuilder = tracer.buildSpan(operationName).asChildOf(parentSpan);

    Utils.startSpanWithFlowNodeTiming(spanBuilder, node);
    completeNodeInformation(spanBuilder, node);

    Span span = spanBuilder.start();
    Utils.addUrlTag(span, node);
    states.put(node, new State(node, span));
    return span;
  }

  private Optional<State> enclosingState(FlowNode node) {
    return Utils.getEnclosing(node).flatMap(e -> Optional.ofNullable(states.get(e)));
  }

  private static boolean isCustomTraceStep(FlowNode node) {
    if (!(node instanceof StepNode)) {
      return false;
    }
    StepNode stepNode = (StepNode) node;
    StepDescriptor descriptor = stepNode.getDescriptor();
    if (descriptor == null) {
      return false;
    }
    return descriptor instanceof DescriptorImpl;
  }

  public void inject(Span span, Map<String, String> envs) {
    tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TextMapAdapter(envs));
  }

  Optional<Span> getSpan(FlowNode node) {
    return Optional.ofNullable(states.get(node)).map(State::getSpan);
  }

  // called *before* ste step has executed
  private static void completeNodeInformation(SpanBuilder builder, FlowNode node) {
    if (!(node instanceof StepNode)) {
      return;
    }
    StepNode stepNode = (StepNode) node;
    StepDescriptor descriptor = stepNode.getDescriptor();
    if (descriptor == null) {
      return;
    }
    builder.withTag("step.functionName", descriptor.getFunctionName());
  }

  // called *after* the step has executed
  private static void completeStartNodeInformation(Span span, BlockStartNode node) {
    StageAction stage = node.getAction(StageAction.class);
    if (stage != null) {
      span.setTag("stage.name", stage.getStageName());
    }

    Map<String, Object> arguments = ArgumentsAction.getFilteredArguments(node);

    arguments.forEach(
        (k, v) -> {
          String tagName = "step.arguments." + k;
          if (!Utils.setTag(span, tagName, v)) {
            Utils.setTag(span, tagName, v.toString());
          }
        });

    getStageName(node, arguments).ifPresent(n -> span.setTag("stage.name", n));
    TagsAction.getTags(node).forEach(span::setTag);
  }

  private static Optional<String> getStageName(FlowNode node, Map<String, Object> arguments) {
    // FIXME make this unnecessary:
    // "https://github.com/jenkinsci/pipeline-stage-step-plugin/pull/13"
    if (node instanceof StepNode) {
      StepDescriptor descriptor = ((StepNode) node).getDescriptor();
      if (descriptor != null) {
        String functionName = descriptor.getFunctionName();
        if ("stage".equals(functionName)) {
          Object nameParameter = arguments.get("name");
          if (nameParameter instanceof String) {
            return Optional.of((String) nameParameter);
          }
        }
      }
    }
    return Optional.empty();
  }

  private static class State {
    private final FlowNode node;
    private final Span span;
    private FlowNode activeChild;
    private boolean finished = false;

    State(FlowNode node, Span span) {
      this.node = node;
      this.span = span;
    }

    Span getSpan() {
      return span;
    }

    FlowNode getNode() {
      return node;
    }

    FlowNode getActiveChild() {
      return activeChild;
    }

    void setActiveChild(FlowNode activeChild) {
      this.activeChild = activeChild;
    }

    void finish(FlowNode finishNode) {
      if (!finished) {
        ErrorAction error = getNode().getError();
        if (error != null) {
          Utils.setError(span, error.getError().getMessage());
        }
        Utils.finishSpanWithFlowNodeTiming(span, finishNode);
      }
      finished = true;
    }
  }
}
