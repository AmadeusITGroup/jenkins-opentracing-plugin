package com.amadeus.jenkins.opentracing.PipelineTracingTest

noop {
    printEnv "spanid"
    printEnv "spanid"
    noop {
    }
    printEnv "spanid"
}
