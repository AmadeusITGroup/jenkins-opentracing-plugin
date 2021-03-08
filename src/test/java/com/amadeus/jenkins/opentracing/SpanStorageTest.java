package com.amadeus.jenkins.opentracing;

import static org.assertj.core.api.Assertions.assertThat;

import com.amadeus.jenkins.opentracing.config.OTConfig;
import com.amadeus.jenkins.opentracing.config.TracerConfig;
import com.amadeus.jenkins.opentracing.test.TestResourceLoader;
import com.amadeus.jenkins.opentracing.test.fixtures.FailStep;
import com.amadeus.jenkins.opentracing.test.fixtures.MockTracerConf;
import com.amadeus.jenkins.opentracing.test.fixtures.NoopStep;
import com.amadeus.jenkins.opentracing.test.fixtures.PrintEnvStep;
import hudson.ExtensionList;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockTracer.Propagator;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.BuildWatcher;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.LoggerRule;
import org.jvnet.hudson.test.TestExtension;

public class SpanStorageTest {
  @ClassRule public static BuildWatcher buildWatcher = new BuildWatcher();
  @Rule public JenkinsRule j = new JenkinsRule();
  @Rule public TestResourceLoader script = new TestResourceLoader("groovy");
  @Rule public LoggerRule logs = new LoggerRule();

  @TestExtension public static final StepDescriptor printEnvStep = PrintEnvStep.extension;

  @TestExtension public static final StepDescriptor noopStep = NoopStep.extension;

  @TestExtension public static final StepDescriptor failStep = FailStep.extension;

  private static final Collection<String> JENKINS_LOGGER_ROOT_PACKAGES =
      Arrays.asList("hudson", "org.jenkins", "org.jenkinsci");

  private MockTracer mockTracer;

  @Before
  public void setUp() {
    ExtensionList.lookupSingleton(SpanStorage.class).setCacheSupplier(BlackHoleMap::new);
    logs.capture(10000);
    JENKINS_LOGGER_ROOT_PACKAGES.forEach(p -> logs.record(p, Level.WARNING));
    assertThat(logs.getMessages()).isEmpty();
    assertThat(logs.getRecords()).isEmpty();
    OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
    mockTracer = new MockTracer(Propagator.TEXT_MAP);
    TracerConfig tracerConfig = new MockTracerConf(mockTracer);

    config.setTracer(tracerConfig);
  }

  @After
  public void assertions() {
    assertThat(logs.getMessages()).isEmpty();
    assertThat(mockTracer.finishedSpans()).isEmpty();
  }

  @Test
  public void testExecutionWithMissingCaches() throws Exception {
    WorkflowRun r = buildPipeline();
  }

  @Test
  public void testFailing() throws Exception {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(script.read(), true));
    WorkflowRun b = TestUtils.notNull(p.scheduleBuild2(0)).get();
  }

  private WorkflowRun buildPipeline(String script) throws Exception {
    WorkflowJob p = j.jenkins.createProject(WorkflowJob.class, "p");
    p.setDefinition(new CpsFlowDefinition(script, true));
    return j.buildAndAssertSuccess(p);
  }

  private WorkflowRun buildPipeline() throws Exception {
    return buildPipeline(script.read());
  }

  private static class BlackHoleMap<K, V> implements Map<K, V> {
    @Override
    public int size() {
      return 0;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public boolean containsKey(Object key) {
      return false;
    }

    @Override
    public boolean containsValue(Object value) {
      return false;
    }

    @Override
    public V get(Object key) {
      return null;
    }

    @Override
    public V put(K key, V value) {
      return null;
    }

    @Override
    public V remove(Object key) {
      return null;
    }

    @Override
    public void putAll(@Nonnull Map<? extends K, ? extends V> m) {}

    @Override
    public void clear() {}

    @Override
    @Nonnull
    public Set<K> keySet() {
      return Collections.emptySet();
    }

    @Override
    @Nonnull
    public Collection<V> values() {
      return Collections.emptySet();
    }

    @Override
    @Nonnull
    public Set<Entry<K, V>> entrySet() {
      return Collections.emptySet();
    }
  }
}
