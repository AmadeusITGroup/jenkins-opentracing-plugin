package com.amadeus.jenkins.opentracing.PipelineTracingTest

def trace() {
    echo "trace function called"
}

noop {
    trace()

    def trace = "foo"
    echo "trace=${trace}"
}