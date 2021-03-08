package com.amadeus.jenkins.opentracing.PipelineTracingTest

noop {
    def a = trace {
        "foo"
    }
    echo a
}
