package com.amadeus.jenkins.opentracing.workflow;

import com.amadeus.jenkins.opentracing.OTFlowExecutionListener;
import com.amadeus.jenkins.opentracing.OTGraphListener;
import com.amadeus.jenkins.opentracing.Utils;
import io.opentracing.Span;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.AbstractStepExecutionImpl;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback.TailCall;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/** Any configuration passed to the step is passed on to the {@link Span} of it's body. */
@Restricted(NoExternalUse.class)
class TraceStepBodyExecution extends AbstractStepExecutionImpl
    implements WorkflowUtils.SpanInformation {

  private static final long serialVersionUID = -5163712628686731682L;

  private final Map<String, String> tags;
  private final String operationName;

  public TraceStepBodyExecution(
      @Nonnull StepContext context,
      @Nullable String operationName,
      @Nullable Map<String, String> tags) {

    super(context);
    this.tags = tags;
    this.operationName = operationName;
  }

  @Override
  public boolean start() throws IOException, InterruptedException {
    final StepContext context = getContext();
    execute(context);
    context.newBodyInvoker().withCallback(new Callback()).start();
    return false;
  }

  private void execute(StepContext context) throws IOException, InterruptedException {
    final OTFlowExecutionListener flowListener =
        Utils.tryLookupSingleton(OTFlowExecutionListener.class);
    if (flowListener == null) {
      return;
    }
    final FlowNode node = context.get(FlowNode.class);
    if (node == null) {
      return;
    }
    final OTGraphListener graphListener = flowListener.getListener(node.getExecution());
    if (graphListener == null) {
      return;
    }
    Span span = graphListener.onNewStartOrAtomNode(node);
    if (span != null) {
      WorkflowUtils.addInformation(span, this);
    }
  }

  @Override
  public Map<String, String> getTags() {
    return tags;
  }

  @Nullable
  @Override
  public String getOperationName() {
    return operationName;
  }

  // NOOP, as the generic pipeline tracing facilities will take care of everything
  private static class Callback extends TailCall {

    private static final long serialVersionUID = -4510710716171458412L;

    @Override
    protected void finished(StepContext context) {
      /* NOOP */
    }
  }
}
