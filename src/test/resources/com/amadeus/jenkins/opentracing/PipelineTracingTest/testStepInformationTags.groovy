package com.amadeus.jenkins.opentracing.PipelineTracingTest

stage("foo") {
    parameters("someName") {
    }
    parameters(name: "name", boolean: true, number: 42, map: [key: "value"]) {
    }
}