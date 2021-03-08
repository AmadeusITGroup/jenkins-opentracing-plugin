package com.amadeus.jenkins.opentracing.PipelineTracingTest

pipeline {
    agent any
    stages {
        stage("foo") {
            steps {
                noop {
                    echo "foo"
                }
                trace(operationName: "someOp", tags: [foo: "bar"]) {
                    echo "bar"
                }
            }
        }
    }
}
