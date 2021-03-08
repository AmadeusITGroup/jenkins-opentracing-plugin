package com.amadeus.jenkins.opentracing.workflow;

import hudson.Extension;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.DoNotUse;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

/**
 * {@link Step} allowing pipeline authors to explicitly supply information to the tracing system.
 * Can be used either with or without a block. @see {@link TraceStepAtomExecution} and {@link
 * TraceStepBodyExecution} for details.
 */
@Extension
@Symbol("trace")
@Restricted(DoNotUse.class)
public final class TraceStep extends Step {
  private List<Tag> tags;
  private String operationName;

  @Override
  public StepExecution start(StepContext context) {
    Map<String, String> tagMap = Tag.toMap(tags);

    if (context.hasBody()) {
      return new TraceStepBodyExecution(context, operationName, tagMap);
    }
    return new TraceStepAtomExecution(context, tagMap);
  }

  public TraceStep() {
    this(null);
  }

  @DataBoundConstructor
  public TraceStep(@Nullable String operationName) {
    this.operationName = operationName;
  }

  @DataBoundSetter
  public void setTags(List<Tag> tags) {
    this.tags = tags;
  }

  public List<Tag> getTags() {
    return tags;
  }

  @DataBoundSetter
  public void setOperationName(String operationName) {
    this.operationName = operationName;
  }

  public String getOperationName() {
    return operationName;
  }

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  @Extension
  public static class DescriptorImpl extends StepDescriptor {

    @Override
    public Set<? extends Class<?>> getRequiredContext() {
      // In practice we always need a FlowNode, but this step should never change the behaviour of
      // the build, including stopping it.
      return Collections.emptySet();
    }

    @Override
    public boolean takesImplicitBlockArgument() {
      return true;
    }

    @Override
    public String getFunctionName() {
      return "trace";
    }

    @Override
    public String getDisplayName() {
      return "Add tracing information";
    }

    @Override
    public Step newInstance(Map<String, Object> arguments) throws Exception {
      Object tags = arguments.get("tags");
      if (tags instanceof Map) {
        //noinspection unchecked
        arguments.put("tags", Tag.fromMap((Map) tags));
      }
      return super.newInstance(arguments);
    }
  }
}
