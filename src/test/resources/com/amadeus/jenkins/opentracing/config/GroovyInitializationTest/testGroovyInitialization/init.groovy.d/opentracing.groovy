package com.amadeus.jenkins.opentracing.config.GroovyInitializationTest.testGroovyInitialization.init.groovy.d

import com.amadeus.jenkins.opentracing.config.OTConfig
import com.amadeus.jenkins.opentracing.config.impl.JaegerConfig
import hudson.ExtensionList

OTConfig config = ExtensionList.lookupSingleton(OTConfig.class);
JaegerConfig jaegerConfig = new JaegerConfig(
    new JaegerConfig.UdpSenderConfig("localhost", 1234),
    "https://localhost:5678",
)
config.setTracer(
    jaegerConfig
)