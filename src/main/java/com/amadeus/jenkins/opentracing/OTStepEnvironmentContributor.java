package com.amadeus.jenkins.opentracing;

import hudson.EnvVars;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.TaskListener;
import io.opentracing.Span;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.graph.AtomNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepEnvironmentContributor;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

@Extension
@Restricted(DoNotUse.class)
public final class OTStepEnvironmentContributor extends StepEnvironmentContributor {

  private final OTFlowExecutionListener flowExecutionListener =
      ExtensionList.lookupSingleton(OTFlowExecutionListener.class);

  @Override
  public void buildEnvironmentFor(
      @Nonnull StepContext stepContext, @Nonnull EnvVars envs, @Nonnull TaskListener listener)
      throws IOException, InterruptedException {

    FlowNode node = stepContext.get(FlowNode.class);
    if (node == null) {
      return;
    }
    if (!(node instanceof AtomNode)) {
      return;
    }
    OTGraphListener c = flowExecutionListener.getListener(node.getExecution());
    if (c == null) {
      return;
    }
    Span span = c.onNewStartOrAtomNode(node);
    if (span == null) {
      return;
    }
    c.inject(span, envs);
  }
}
