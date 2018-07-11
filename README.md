# nightswatch
/prof web endpoint for java [async-profiler]

## compile
mvn clean install

## run
export ASYNC_PROFILER_HOME=/path/to/async-profiler/release

To start the server (default port is 9898), run nightswatch script

```./nightswatch```

## usage

- To collect CPU profile for a specific PID

```curl "http://localhost:9898/prof?pid=12920"```

![CPU FlameGraph](https://raw.githubusercontent.com/prasanthj/nightswatch/master/img/cpu-flamegraph.svg)


- To collect CPU profile for a specific PID and output in tree format (html)

```curl "http://localhost:9898/prof?output=tree&pid=12920"```

![CPU Tree View](https://raw.githubusercontent.com/prasanthj/nightswatch/master/img/lock-tree.png)


- To collect heap allocation profile for a specific PID (replace the PID below with appropriate process) for 30s default duration. Following command returns [FlameGraph] output in svg format

```curl "http://localhost:9898/prof?event=alloc&pid=12920"```

![Heap Allocation FlameGraph](https://raw.githubusercontent.com/prasanthj/nightswatch/master/img/alloc-flamegraph.svg)


- To collect lock contention profile for a specific PID (replace the PID below with appropriate process) for 10s

```curl "http://localhost:9898/prof?event=lock&pid=12920&duration=10"```

![Lock Contention FlameGraph](https://raw.githubusercontent.com/prasanthj/nightswatch/master/img/lock-flamegraph.svg)


[async-profiler]:https://github.com/jvm-profiling-tools/async-profiler
[FlameGraph]:https://github.com/brendangregg/FlameGraph
