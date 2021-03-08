package com.amadeus.jenkins.opentracing;

import static com.amadeus.jenkins.opentracing.TestUtils.notNull;
import static com.amadeus.jenkins.opentracing.test.MockSpanAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.config.TracerUiLink;
import com.amadeus.jenkins.opentracing.test.OutputCollector;
import com.amadeus.jenkins.opentracing.test.TestResourceLoader;
import com.amadeus.jenkins.opentracing.test.fixtures.FailStep;
import com.amadeus.jenkins.opentracing.test.fixtures.MockTracerConf;
import com.amadeus.jenkins.opentracing.test.fixtures.NodeTaggerStep;
import com.amadeus.jenkins.opentracing.test.fixtures.NoopStep;
import com.amadeus.jenkins.opentracing.test.fixtures.ParamsStep;
import com.amadeus.jenkins.opentracing.test.fixtures.PrintEnvStep;
import hudson.ExtensionList;
import hudson.PluginManager;
import hudson.PluginWrapper;
import hudson.model.Result;
import hudson.util.VersionNumber;
import io.opentracing.mock.MockSpan;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import io.opentracing.tag.Tags;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import org.jenkinsci.plugins.scriptsecurity.sandbox.Whitelist;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.BlanketWhitelist;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.junit.After;
import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

public class PipelineTracingTest {
  @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule public OutputCollector outputCollector = new OutputCollector();
  @Rule public JenkinsRule j = new JenkinsRule();
  @Rule public TestResourceLoader script = new TestResourceLoader("groovy");

  @TestExtension public static final StepDescriptor printEnvStep = PrintEnvStep.extension;

  @TestExtension public static final StepDescriptor noopStep = NoopStep.extension;

  @TestExtension public static final StepDescriptor failStep = FailStep.extension;

  @TestExtension public static final StepDescriptor paramsStep = ParamsStep.extension;

  @TestExtension public static final StepDescriptor nodeTaggerStep = NodeTaggerStep.extension;

  private List<MockSpan> spans;

  @Before
  public void setUp() {
    assertThat(ExtensionList.lookup(OTConfig.class)).hasSize(1);
    MockTracer mockTracer = new MockTracer(Propagator.TEXT_MAP);
    TracerConfig tracerConfig = new MockTracerConf(mockTracer);

    ExtensionList.lookupSingleton(OTConfig.class).setTracer(tracerConfig);
  }

  @After
  public void finalChecks() {
    if (spans == null) {
      return;
    }
    for (MockSpan span : spans) {
      assertThat(span.context().baggageItems()).isEmpty();
      assertThat(span).hasTag("jenkins.rooturl");
    }
  }

  private static void validateRecordedOutput(ByteArrayOutputStream record) {}

  @Test
  public void testTestSetup() throws Exception {
    WorkflowRun b = buildPipeline("");
    assertThat(pipelineSpans()).isEmpty();
  }

  @Test
  public void testSingleAtomStep() throws Exception {
    // https://issues.jenkins-ci.org/browse/JENKINS-52189
    assumePluginVersionGreaterOrEqual("workflow-cps", "2.63");

    WorkflowRun b = buildPipeline();
    j.assertLogContains("spanid=", b);
    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(1);
    assertThat(spans).extracting(MockSpan::operationName).containsOnlyOnce("printEnv");
  }

  @Test
  public void testSingleAtomStepInBlockStep() throws Exception {
    WorkflowRun b = buildPipeline();
    List<MockSpan> spans = pipelineSpans();

    assertThat(spans).hasSize(3);
    MockSpan printEnv = spans.get(0);
    MockSpan noopBodyStart = spans.get(1);
    MockSpan noopStart = spans.get(2);

    assertThat(printEnv.operationName()).matches("printEnv");
    assertThat(noopBodyStart.operationName()).contains("Body").contains("NoopStep");
    assertThat(noopStart.operationName()).doesNotContain("Body").contains("NoopStep");

    assertThat(printEnv).isTemporallyEnclosedDirectChildOf(noopBodyStart);
    assertThat(noopBodyStart).isTemporallyEnclosedDirectChildOf(noopStart);
  }

  @Test
  public void testNextStepFinishesPreviousAtomStep() throws Exception {
    WorkflowRun b = buildPipeline();
    List<MockSpan> spans = pipelineSpans();

    assertThat(spans).hasSize(7);
    MockSpan printEnv1 = spans.get(0);
    MockSpan printEnv2 = spans.get(1);
    MockSpan innerNoopBodyStart = spans.get(2);
    MockSpan innerNoopStart = spans.get(3);
    MockSpan printEnv3 = spans.get(4);
    MockSpan outerNoopBodyStart = spans.get(5);
    MockSpan outerNoopStart = spans.get(6);

    assertThat(printEnv1).isSibling(printEnv2, innerNoopStart, printEnv3);
    assertThat(printEnv2).isTemporallyAfter(printEnv1);
    assertThat(printEnv3).isTemporallyAfter(printEnv2);
    assertThat(innerNoopStart).isTemporallyAfter(printEnv2);
    assertThat(printEnv3).isTemporallyAfter(innerNoopStart);
  }

  @Test
  public void testFailedAtomNodeGetsErrorTag() throws Exception {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(script.read(), true));
    WorkflowRun b = notNull(p.scheduleBuild2(0)).get();

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(3);

    MockSpan fail = spans.get(0);
    MockSpan noopBodyStart = spans.get(1);
    MockSpan noopStart = spans.get(2);

    assertThat(fail.operationName()).contains("fail");
    assertThat(fail).hasTag("error", true);
  }

  @Test
  public void testCustomTraceStep() throws Exception {
    WorkflowRun b = buildPipeline();
    List<MockSpan> spans = pipelineSpans();

    assertThat(spans).hasSize(7);

    MockSpan printEnv = spans.get(0);
    MockSpan innerTraceBodyStart1 = spans.get(1);
    MockSpan innerTraceStart1 = spans.get(2);
    MockSpan innerTraceBodyStart2 = spans.get(3);
    MockSpan innerTraceStart2 = spans.get(4);
    MockSpan outerTraceBodyStart = spans.get(5);
    MockSpan outerTraceStart = spans.get(6);

    assertThat(innerTraceStart1).isTemporallyEnclosedDirectChildOf(outerTraceBodyStart);
    assertThat(innerTraceStart2).isTemporallyEnclosedDirectChildOf(outerTraceBodyStart);
    assertThat(printEnv).isTemporallyEnclosedDirectChildOf(innerTraceBodyStart1);

    assertThat(innerTraceStart1).hasTag("baz", "quux");
    assertThat(innerTraceStart1.operationName()).isEqualTo("Operation");
    assertThat(outerTraceStart).hasTag("foo", "bar");
    assertThat(printEnv).hasNoUncommonTags();
    assertThat(innerTraceBodyStart1).hasNoUncommonTags();
    assertThat(outerTraceBodyStart).hasNoUncommonTags();

    assertThat(innerTraceStart2.operationName()).isEqualTo("SomeImportantOperation");
    assertThat(innerTraceStart2).hasNoUncommonTags();
  }

  @Test
  public void testCustomTraceStepReturnsBody() throws Exception {
    WorkflowRun b = buildPipeline();
    assertThat(b.getLog(1000)).contains("foo");
  }

  @Test
  public void testCustomTraceStepAtom() throws Exception {
    WorkflowRun b = buildPipeline();
    List<MockSpan> spans = pipelineSpans();

    assertThat(spans).hasSize(3);

    MockSpan printEnv = spans.get(0);
    MockSpan noopBodyStart = spans.get(1);
    MockSpan noopStart = spans.get(2);

    assertThat(printEnv.operationName()).contains("printEnv");
    assertThat(printEnv).hasNoUncommonTags();

    assertThat(noopBodyStart.operationName()).contains("NoopStep").contains("Body");
    assertThat(noopBodyStart)
        .hasOnlyUncommonTags("baz", "bli", "abc", "aaa", "putAt", "assignFromMap");
  }

  @TestExtension("coverageCustomTraceStepAtomTags")
  public static final Whitelist coverageWhiteList = new BlanketWhitelist();

  @Test
  public void coverageCustomTraceStepAtomTags() throws Exception {
    WorkflowRun b = buildPipeline();
  }

  @Test
  public void testUiLink() throws Exception {
    WorkflowRun b = buildPipeline("");
    assertThat(b.getAction(TracerUiLink.class)).isNotNull();
  }

  @Test
  public void testInject() throws Exception {
    // https://issues.jenkins-ci.org/browse/JENKINS-51170
    assumePluginVersionGreaterOrEqual("workflow-step-api", "2.19");
    assumePluginVersionGreaterOrEqual("workflow-support", "3.2");
    assumePluginVersionGreaterOrEqual("workflow-cps", "2.63");

    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans();

    assertThat(spans).hasSize(3);

    MockSpan printEnv = spans.get(0);
    assertThat(printEnv.operationName()).matches("printEnv");

    long spanId = printEnv.context().spanId();
    String pattern = "spanid=" + spanId;
    assertThat(JenkinsRule.getLog(b)).contains(pattern);
  }

  @Test
  public void testNodeStepQueueing() throws Exception {
    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans(false);

    MockSpan waiting1 = getSpan(spans, 0, "Waiting");
    MockSpan queue = getSpan(spans, 1, "Queue p");
    MockSpan waiting2 = getSpan(spans, 2, "Waiting");
    MockSpan buildable = getSpan(spans, 3, "Buildable");
    MockSpan queue2 = getSpan(spans, 4, "Queue part of p");
    MockSpan printEnv = getSpan(spans, 5, "printEnv");
    MockSpan allocateBody = getSpan(spans, 6, "Allocate node : Body : Start");
    MockSpan allocate = getSpan(spans, 7, "Allocate node : Start");
    MockSpan noopBody = getSpan(spans, 8, "NoopStep : Body : Start");
    MockSpan noop = getSpan(spans, 9, "NoopStep : Start");
    // MockSpan pipeline = getSpan(spans, 10, "Start of Pipeline Start of Pipeline");
    MockSpan job = getSpan(spans, spans.size() - 1, "Job #1");

    assertThat(allocate).isDirectChildOf(noopBody);
    assertThat(queue2).isDirectChildOf(allocate);
    // FIXME should this be the parent of "buildable" or the queue itself?
    assertThat(allocateBody).isDirectChildOf(queue2);
  }

  @Test
  public void testErrorTag() throws Exception {

    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(script.read(), true));
    WorkflowRun b = notNull(p.scheduleBuild2(0)).get();

    assertThat(b.getResult()).isEqualTo(Result.FAILURE);

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(5);

    MockSpan fail = getSpan(spans, 0, "fail");
    MockSpan innerNoopBody = getSpan(spans, 1, "NoopStep : Body");
    MockSpan innerNoop = getSpan(spans, 2, "NoopStep : Start");
    MockSpan outerNoopBody = getSpan(spans, 3, "NoopStep : Body");
    MockSpan outerNoop = getSpan(spans, 4, "NoopStep : Start");

    assertThat(fail)
        .hasTag(Tags.ERROR.getKey(), true)
        .isTemporallyEnclosedDirectChildOf(innerNoopBody);
    assertThat(innerNoopBody).hasNoUncommonTags().isTemporallyEnclosedDirectChildOf(innerNoop);
    assertThat(innerNoop).hasNoUncommonTags().isTemporallyEnclosedDirectChildOf(outerNoopBody);
    assertThat(outerNoopBody).hasNoUncommonTags().isTemporallyEnclosedDirectChildOf(outerNoop);

    List<MockSpan> allSpans = pipelineSpans(false);
    MockSpan job = getSpan(allSpans, allSpans.size() - 1, "Job");
    assertThat(job).hasTag(Tags.ERROR.getKey(), true);
  }

  @Test
  public void testStepInformationTags() throws Exception {
    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(6);

    MockSpan innerParamsStepBody1 = getSpan(spans, 0, "ParamsStep : Body");
    MockSpan innerParamsStep1 = getSpan(spans, 1, "ParamsStep : Start");
    MockSpan innerParamsStepBody2 = getSpan(spans, 2, "ParamsStep : Body");
    MockSpan innerParamsStep2 = getSpan(spans, 3, "ParamsStep : Start");
    MockSpan stageBody = getSpan(spans, 4, "foo");
    MockSpan stage = getSpan(spans, 5, "Start stage");

    assertThat(innerParamsStep1)
        .hasTag("step.functionName", "parameters")
        .hasTag("step.arguments.name", "someName");
    assertThat(innerParamsStep2)
        .hasTag("step.functionName", "parameters")
        .hasTag("step.arguments.number", 42)
        .hasTag("step.arguments.boolean", true)
        .hasTag("step.arguments.name", "name")
        .hasTag("step.arguments.map");
    assertThat(stage)
        .hasTag("step.functionName", "stage")
        .hasTag("stage.name", "foo")
        .hasTag("step.arguments.name", "foo");
  }

  @Test
  public void testTraceTagDoesNotOverrideExistingBindings() throws Exception {
    WorkflowRun b = buildPipeline();

    j.assertLogContains("trace=foo", b);
    j.assertLogContains("trace function called", b);
  }

  @Test
  public void testDeclarative() throws Exception {
    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(12);

    MockSpan someOp = getSpan(spans, 7, "someOp");
    assertThat(someOp).hasTag("foo", "bar");
  }

  @Test
  public void testTagsAction() throws Exception {
    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(2);
    MockSpan nodeTagger = getSpan(spans, 1, "NodeTaggerStep : Start");
    assertThat(nodeTagger).hasTag("foo", "bar");
  }

  @Test
  public void testParallelSteps() throws Exception {
    WorkflowRun b = buildPipeline();

    List<MockSpan> spans = pipelineSpans();
    assertThat(spans).hasSize(9);

    MockSpan echo0 = getSpan(spans, 0, "echo");
    MockSpan branch0 = getSpan(spans, 1, "branch0");
    assertThat(echo0).isTemporallyEnclosedDirectChildOf(branch0);

    MockSpan echo1 = getSpan(spans, 2, "echo");
    MockSpan branch1 = getSpan(spans, 3, "branch1");
    assertThat(echo1).isTemporallyEnclosedDirectChildOf(branch1);

    MockSpan echo2 = getSpan(spans, 4, "echo");
    MockSpan branch2 = getSpan(spans, 5, "branch2");
    assertThat(echo2).isTemporallyEnclosedDirectChildOf(branch2);

    MockSpan echo3 = getSpan(spans, 6, "echo");
    MockSpan branch3 = getSpan(spans, 7, "branch3");
    assertThat(echo3).isTemporallyEnclosedDirectChildOf(branch3);

    MockSpan parallel = getSpan(spans, 8, "Start parallel");
    for (MockSpan branch : Arrays.asList(branch0, branch1, branch2, branch3)) {
      assertThat(branch).isTemporallyEnclosedDirectChildOf(parallel);
    }
  }

  @Test
  public void testExecutionWithoutConfiguration() throws Exception {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);

    config.setTracer(null);
    assertThat(config.getTracerForName("foo").toString())
        .contains("DelegatingTracer")
        .contains("delegate=NoopTracer");

    WorkflowRun b = buildPipeline();

    assertThat(outputCollector.getErr().toString())
        .isNotBlank()
        .doesNotContain("Exception")
        .doesNotContain("com.amadeus.opentracing");
  }

  private WorkflowRun buildPipeline(String script) throws Exception {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(script, true));
    return j.buildAndAssertSuccess(p);
  }

  private WorkflowRun buildPipeline() throws Exception {
    return buildPipeline(script.read());
  }

  private List<MockSpan> pipelineSpans() {
    return pipelineSpans(true);
  }

  private List<MockSpan> pipelineSpans(boolean removeQueuing) {
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    TracerConfig tracerConfig = config.getTracer();
    MockTracer mockTracer = ((MockTracerConf) tracerConfig).getTracer();
    List<MockSpan> spans = mockTracer.finishedSpans();
    ListIterator<MockSpan> iterator = spans.listIterator();
    while (iterator.hasNext()) {
      MockSpan span = iterator.next();
      assertThat(span.generatedErrors()).isEmpty();
      MockSpan.MockContext context = span.context();

      long traceId = context.traceId();
      long spanId = context.spanId();
      long parentId = span.parentId();
      String operationName = span.operationName();

      if ((
          /*spanId == 2 && parentId == 0 && */ operationName.equals("Queue p"))
          || (
          /*spanId == 3 && parentId == 2 && */ operationName.equals("Waiting"))
          || (
          /*spanId == 4 && parentId == 2 && */ operationName.equals("Job #1"))
          || operationName.equals("Start of Pipeline Start of Pipeline")) {
        if (removeQueuing) {
          iterator.remove();
        }
      }
    }
    this.spans = spans;
    return spans;
  }

  private MockSpan getSpan(List<MockSpan> spans, int index, String pattern) {
    MockSpan span = spans.get(index);
    assertThat(span.operationName()).contains(pattern);
    return span;
  }

  @Test
  public void testPluginAssumptionTesting() {
    // we don't test for our own plugin, as on CI we get weird version numbers
    assertThatExceptionOfType(AssumptionViolatedException.class)
        .isThrownBy(() -> assumePluginVersionGreaterOrEqual("workflow-step-api", "99"));

    assertThatCode(() -> assumePluginVersionGreaterOrEqual("workflow-step-api", "0.0.1"))
        .doesNotThrowAnyException();
  }

  private void assumePluginVersionGreaterOrEqual(String pluginName, String expectedVersion) {
    PluginManager pluginManager = j.getPluginManager();

    PluginWrapper plugin = pluginManager.getPlugin(pluginName);
    Assume.assumeTrue(String.format("Plugin %s is not installed", pluginName), plugin != null);

    VersionNumber installedVersion = plugin.getVersionNumber();
    Assume.assumeFalse(
        String.format("Plugin %s has version >= %s", pluginName, expectedVersion),
        installedVersion.isOlderThan(new VersionNumber(expectedVersion)));
  }
}
