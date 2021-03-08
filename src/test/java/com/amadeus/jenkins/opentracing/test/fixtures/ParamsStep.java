package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.SimpleAsynchronousStepExecution;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class ParamsStep extends Step {
  @DataBoundConstructor
  public ParamsStep(@Nullable String name) {}

  @DataBoundSetter
  public void setBoolean(boolean value) {}

  @DataBoundSetter
  public void setNumber(Number value) {}

  @DataBoundSetter
  public void setString(String value) {}

  @DataBoundSetter
  public void setMap(Map<String, String> value) {}

  @Override
  public StepExecution start(StepContext context) {
    return SimpleAsynchronousStepExecution.noop(context);
  };

  public static class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "parameters";
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
