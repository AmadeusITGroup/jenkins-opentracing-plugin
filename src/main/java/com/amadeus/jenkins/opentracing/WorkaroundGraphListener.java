package com.amadeus.jenkins.opentracing;

import hudson.Extension;
import hudson.ExtensionList;
import org.jenkinsci.plugins.workflow.flow.GraphListener;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graph.FlowStartNode;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;

/**
 * This class works around https://issues.jenkins-ci.org/browse/JENKINS-52189 Especially the part
 * where {@link GraphListener}s attached during {@link
 * org.jenkinsci.plugins.workflow.flow.FlowExecutionListener#onRunning} do not receive the
 * FlowStartNode. As the per instance logic is nicer to reason about and may work properly in a
 * future version of Jenkins, this class will be the bridge until then.
 */
@Extension
@Restricted(DoNotUse.class)
public final class WorkaroundGraphListener implements GraphListener, GraphListener.Synchronous {
  private final OTFlowExecutionListener flowExecutionListener =
      ExtensionList.lookupSingleton(OTFlowExecutionListener.class);

  @Override
  public void onNewHead(FlowNode node) {
    if (node instanceof FlowStartNode) {
      OTGraphListener listener = flowExecutionListener.getListener(node.getExecution());
      if (listener != null) {
        listener.onNewHead(node);
      }
    }
  }
}
