package com.amadeus.jenkins.opentracing.PipelineTracingTest

noop {
    def t = trace()
    t.setTag("1", "1")
    t.setTag("2", 2)
    t.setTag("3", true)

    def fields = [:]
    t.log(fields)
    t.log(15, fields)
    t.log(java.time.Instant.now(), fields)
    t.log("someEvent")
    t.log(15, "someEvent")
    t.log(java.time.Instant.now(), "someEvent")

    def tags = t.tags
    tags["foo"] = 1
    tags["bar"] = true
    tags["baz"] = "baz"

    tags + [
        "4": "4",
        "5": 5,
        "6": true,
        "7": [],
    ]
}