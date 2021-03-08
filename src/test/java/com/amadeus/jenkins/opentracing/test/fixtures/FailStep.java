package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.SimpleSynchronousStepExecution;
import java.util.Collections;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class FailStep extends Step {
  @DataBoundConstructor
  public FailStep(String var) {}

  @Override
  public StepExecution start(StepContext context) {
    return new SimpleSynchronousStepExecution(
        context,
        exe -> {
          throw new IllegalStateException("fail failed, really!");
        });
  }

  public static class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "fail";
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return Collections.emptySet();
    }
  }

  public static DescriptorImpl extension = new DescriptorImpl();
}
