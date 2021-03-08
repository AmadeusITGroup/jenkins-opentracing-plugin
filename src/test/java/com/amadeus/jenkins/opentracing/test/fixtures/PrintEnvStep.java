package com.amadeus.jenkins.opentracing.test.fixtures;

import com.amadeus.jenkins.opentracing.test.fixtures.StepUtils.SimpleSynchronousStepExecution;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import hudson.EnvVars;
import hudson.model.TaskListener;
import java.util.Set;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

public class PrintEnvStep extends Step {
  private final String var;

  @DataBoundConstructor
  public PrintEnvStep(String var) {
    this.var = var;
  }

  @Override
  public StepExecution start(StepContext context) {
    return new SimpleSynchronousStepExecution(
        context,
        exe -> {
          StepContext ctx = exe.getContext();
          String message;
          if (var != null) {
            message = this.var + "=" + ctx.get(EnvVars.class).get(var);
          } else {
            message = Joiner.on('\n').withKeyValueSeparator("=").join(ctx.get(EnvVars.class));
          }
          ctx.get(TaskListener.class).getLogger().println(message);
        });
  }

  public static class DescriptorImpl extends StepDescriptor {
    @Override
    public String getFunctionName() {
      return "printEnv";
    }

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      return ImmutableSet.of(EnvVars.class, TaskListener.class);
    }
  }

  public static DescriptorImpl extension = new DescriptorImpl();
}
