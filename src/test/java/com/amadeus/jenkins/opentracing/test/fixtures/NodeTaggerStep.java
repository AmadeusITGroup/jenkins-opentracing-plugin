package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.CallBack;
import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.SimpleAsynchronousStepExecution;
import java.util.Collections;
import java.util.Set;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class NodeTaggerStep extends Step {
  private final String name;
  private final String value;

  @DataBoundConstructor
  public NodeTaggerStep(@Nonnull String name, @Nonnull String value) {
    this.name = name;
    this.value = value;
  }

  @Override
  public StepExecution start(StepContext context) {
    return new SimpleAsynchronousStepExecution(context, new Execution(name, value));
  };

  private static class Execution implements CallBack {
    private final String name;
    private final String value;

    private Execution(String name, String value) {
      this.name = name;
      this.value = value;
    }

    @Override
    public void apply(StepExecution exe) throws Exception {
      FlowNode node = exe.getContext().get(FlowNode.class);
      TagsAction action = node.getAction(TagsAction.class);
      if (action == null) {
        action = new TagsAction();
      }
      action.addTag(name, value);
      node.addOrReplaceAction(action);
    }
  }

  public static class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "nodeTagger";
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return Collections.emptySet();
    };

    @Override
    public boolean takesImplicitBlockArgument() {
      return true;
    }
  }

  public static DescriptorImpl extension = new DescriptorImpl();
}
