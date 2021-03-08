package com.amadeus.jenkins.opentracing;

import static org.assertj.core.api.Assertions.assertThat;

import hudson.PluginWrapper;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class UtilsTest {
  @Rule public JenkinsRule j = new JenkinsRule();

  @Test
  public void testPluginShortName() {
    // this test validates that the plugin shortname hardcoded in Utils.java is the correct one.
    // It assumes that the plugin under test is the first one returned by the Jenkins plugin manager
    // Should this test fail because the names do not match it, the name of the plugin has probably
    // changed.
    // Please adapt the hardcoded value in Utils.java

    PluginWrapper ourPlugin = j.getPluginManager().getPlugins().get(0);
    assertThat(ourPlugin).isNotNull();
    String ourShortName = ourPlugin.getShortName();

    assertThat(Utils.PLUGIN_SHORT_NAME).isEqualTo(ourShortName);
  }
}
