package com.amadeus.jenkins.opentracing.SpanStorageTest

noop {
    trace {
        node {
            printEnv "foo"
        }
    }
    node {
        printEnv "foo"
    }
}