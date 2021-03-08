package com.amadeus.jenkins.opentracing.PipelineTracingTest

trace(tags: [foo: "bar"]) {
    trace(operationName: "Operation", tags: [baz: "quux"]) {
        printEnv "abcde"
    }
    trace("SomeImportantOperation") {

    }
}