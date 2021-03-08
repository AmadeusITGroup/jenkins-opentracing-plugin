# Jenkins OpenTracing Plugin

This plugin exposes the Queue-, Build- and Pipeline-Lifecycle of Jenkins to a tracing system.
(Currently Jaeger tracing is supported)

## What does this plugin do

This plugin hooks into various lifecycles of Jenkins, collects and correlates the performed actions
and submits this information to a dedicated tracing server. The tracing context is also propagated
via environment variables to all commands started so that OpenTracing-aware commands can contribute
their own custom traces.

Custom tracing implementations can be plugged in by providing Extensions implementing
`com.amadeus.jenkins.opentracing.config.TracerConfig`.

## Prerequisites

The plugin needs Jenkins pipeline to be installed and network access to the relevant tracing server

## Configuration

The plugin can be configured via the Jenkins UI "Global Configuration" section.
Currently it is advised to restart Jenkins after tracer reconfiguration.
(The hot reconfiguration is experimental).

The plugin can be configured via Configuration-as-Code.
For examples see `src/test/resources/com/amadeus/jenkins/opentracing/config/`.
