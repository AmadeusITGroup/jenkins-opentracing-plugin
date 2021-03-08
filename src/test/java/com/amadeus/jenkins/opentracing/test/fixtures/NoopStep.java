package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.SimpleAsynchronousStepExecution;
import java.util.Collections;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class NoopStep extends Step {
  @DataBoundConstructor
  public NoopStep() {}

  @Override
  public StepExecution start(StepContext context) throws Exception {
    return SimpleAsynchronousStepExecution.noop(context);
  };

  public static class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "noop";
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
