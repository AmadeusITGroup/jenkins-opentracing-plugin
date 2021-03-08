package com.amadeus.jenkins.opentracing.test.fixtures;

import java.io.Serializable;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

public class StepUtils {
  @FunctionalInterface
  public interface CallBack extends Serializable {
    void apply(StepExecution execution) throws Exception;
  }

  public static class SimpleAsynchronousStepExecution extends StepExecution {
    private final CallBack onStart;

    public SimpleAsynchronousStepExecution(StepContext context, CallBack onStart) {
      super(context);
      this.onStart = onStart;
    }

    public static SimpleAsynchronousStepExecution noop(StepContext context) {
      return new SimpleAsynchronousStepExecution(context, new NoopCb());
    }

    private static class NoopCb implements CallBack {

      @Override
      public void apply(StepExecution execution) {}
    }

    @Override
    public boolean start() throws Exception {
      onStart.apply(this);
      getContext().newBodyInvoker().withCallback(new Callback()).start();
      return false;
    }

    private static class Callback extends BodyExecutionCallback {

      @Override
      public void onSuccess(StepContext context, Object result) {
        context.onSuccess(result);
      }

      @Override
      public void onFailure(StepContext context, Throwable t) {
        context.onFailure(t);
      };
    }
  }

  public static class SimpleSynchronousStepExecution extends SynchronousStepExecution<Void> {
    private final CallBack cb;

    public SimpleSynchronousStepExecution(StepContext context, CallBack cb) {
      super(context);
      this.cb = cb;
    }

    @Override
    protected Void run() throws Exception {
      cb.apply(this);
      return null;
    }

    @Nonnull
    @Override
    public StepContext getContext() {
      return super.getContext();
    }
  }
}
