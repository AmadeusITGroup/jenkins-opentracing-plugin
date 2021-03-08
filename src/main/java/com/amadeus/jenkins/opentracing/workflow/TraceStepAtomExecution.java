package com.amadeus.jenkins.opentracing.workflow;

import com.amadeus.jenkins.opentracing.OTFlowExecutionListener;
import com.amadeus.jenkins.opentracing.OTGraphListener;
import com.amadeus.jenkins.opentracing.Utils;
import hudson.model.TaskListener;
import io.opentracing.Span;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

/**
 * Any configuration parameters passed are added to the {@link Span} of the currently active {@link
 * org.jenkinsci.plugins.workflow.steps.Step}. It then also return a {@link WorkflowSpan} that can
 * be used to further configure the enclosing span.
 */
@Restricted(NoExternalUse.class)
final class TraceStepAtomExecution extends SynchronousStepExecution<WorkflowSpan>
    implements WorkflowUtils.SpanInformation {

  private static final long serialVersionUID = 2205795792072338316L;

  private final Map<String, String> tags;

  public TraceStepAtomExecution(@Nonnull StepContext context, @Nullable Map<String, String> tags) {

    super(context);
    this.tags = tags;
  }

  @Override
  protected @Nullable WorkflowSpan run() throws IOException, InterruptedException {
    final StepContext context = getContext();
    final OTFlowExecutionListener flowListener =
        Utils.tryLookupSingleton(OTFlowExecutionListener.class);
    if (flowListener == null) {
      return null;
    }
    final FlowNode node = context.get(FlowNode.class);
    if (node == null) {
      return null;
    }
    final OTGraphListener graphListener = flowListener.getListener(node.getExecution());
    if (graphListener == null) {
      return null;
    }
    Optional<FlowNode> enclosing = Utils.getEnclosing(node);

    if (enclosing.isPresent()) {
      Span span = graphListener.onNewStartOrAtomNode(enclosing.get());
      if (span != null) {
        WorkflowUtils.addInformation(span, this);
        return new WorkflowSpan(span, context.get(TaskListener.class));
      }
    }
    return null;
  }

  @Override
  public Map<String, String> getTags() {
    return tags;
  }

  @Nullable
  @Override
  public String getOperationName() {
    return null;
  }
}
