package com.amadeus.jenkins.opentracing;

import static org.assertj.core.api.Assertions.assertThat;

import hudson.PluginWrapper;
import jenkins.YesNoMaybe;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class PluginLoadingTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void testNotDynamicLoadable() {
    TestUtils.assumePluginManagerInitialized(j);

    PluginWrapper plugin = j.getPluginManager().getPlugin(Utils.PLUGIN_SHORT_NAME);
    System.out.println(plugin);
    assertThat(plugin).isNotNull();
    assertThat(plugin.supportsDynamicLoad()).isEqualTo(YesNoMaybe.NO);
  }
}
