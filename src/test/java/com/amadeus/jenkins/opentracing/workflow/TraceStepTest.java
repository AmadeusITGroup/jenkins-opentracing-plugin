package com.amadeus.jenkins.opentracing.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.jenkinsci.plugins.workflow.steps.StepConfigTester;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class TraceStepTest {

  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void testConfigurationRoundTrip() throws Exception {
    List<Tag> tags = Arrays.asList(new Tag("foo", "bar"), new Tag("a", "b"));
    StepConfigTester tester = new StepConfigTester(j);

    TraceStep step = new TraceStep();

    step.setTags(tags);
    step.setOperationName("foo");

    TraceStep after = tester.configRoundTrip(step);

    assertThat(after.getOperationName()).isEqualTo("foo");
    assertThat(after.getTags()).isNotSameAs(tags).containsExactlyElementsOf(tags);
    j.assertEqualDataBoundBeans(step, after);
  }
}
