package com.amadeus.jenkins.opentracing.PipelineTracingTest

noop {
    def span = trace(tags: [baz: "quux"])
    span.setTag("bli", "bla").setTag("abc", "def")
    def span2 = trace()
    span2.setTag("aaa", "bbb")
    printEnv "spanId"
    trace().tags["putAt"] = "foo"
    trace().tags + [
        assignFromMap: "someValue",
    ]
}
