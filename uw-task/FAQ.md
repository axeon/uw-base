#### 1.简述一下uw-task的主要功能和特性

 uw-task是一个分布式任务框架，通过uw-task可以快速构建分布式任务体系，支持定时任务和队列任务，同时支持任务运维监控和报警设置。 

1. 基于spring boot实现，依赖rabbitMQ,redis。
2. 完全分布式，支持混合云，可指定主机或指定集群运行。
3. 支持定时任务，并支持服务端动态配置。
4. 支持队列任务，支持各种流量控制，支持错误重试，并支持服务端动态配置。
5. 支持RPC风格调用，支持错误重试，并支持服务器端动态配置。
6. 支持多种规则的任务报警。

#### 2.服务是否开启执行任务，需要修改什么配置？

配置文件修改uw.task.enable-task-registry属性。

```yml
# 如果是任务执行主机，此配置应为true。
  enable-task-registry: true
```

#### 3.配置属性task-project和run-target有什么作用？

task-project： 任务项目，必须是包名前缀，用于扫描任务注册。

run-target：运行目标，这是重要参数，用于识别任务执行集群，默认为default。

运行目标是非常重要的参数，它和taskProject一起决定了任务配置的同步联系。

任务可以在运行期，在TaskData中指定运行目标，来指定执行具体任务的服务器集群。

#### 4.设定定时任务和队列任务分别需要继承什么类？实现什么方法？

定时任务：继承TaskCroner类实现runTask方法。

队列任务：继承TaskRunner类实现runTask方法。

#### 5.队列任务的线程模型是怎样的？

单一实例，类的属性是多线程共享的。

#### 6.uw-task的任务入口类是哪个？其中sendToQueue/runTask/runQueue/...方法有什么差别？什么样的任务优先考虑本地执行？

TaskFactory.java

runTask: 同步执行任务，可能会导致堵塞。运行期，程序根据runType判断，并结合任务代码是否在本地的判定，决定是运行在本地还是远程。 一般来说，程序的默认runType=RUN_TYPE_AUTO_RPC，此时是自动判定默认。 也可以指定RUN_TYPE_GLOBAL_RPC和RUN_TYPE_LOCAL。 

runTaskAsync：方法上与runTask几乎一致，但是是异步运行任务，用future来获取方法的返回值。
此方法要谨慎使用，因为task存在限速，大并发下可能会导致线程数超。

计算量小耗时短的任务应优先考虑本地执行，降低网络io等开销。

#### 7.如果发现任务不能注册，有可能是什么原因？

 可能是任务上有没有设置@Componet注解，uw-task的任务通过Spring的@Componet注解扫描识别并注入系统，或者任务没有写在task-project指定的包里。

#### 8.uw-task的异常抛出有几种？分别对应什么场景？会造成什么影响?

TaskDataException：数据异常。出现TaskDataException，说明是数据错误。传入了错误或者不合法的任务参数，或者任务中获取到的一些数据也不合法的时候抛出。

TaskPartnerException：合作方异常。出现TaskPartnerException，说明是合作方的错误。，此异常需要程序员手工抛出。一般http超时，返回码错误（非200）肯定要抛接口方异常的，其他可能为接口方异常的，可自行决断抛出。 

其它程序异常：一般来说，尽量不要捕获异常，除非这个异常捕获后不影响任务的完整执行。不捕获的异常，框架会自动捕获为程序异常。 

程序会根据抛出的异常设置任务的执行状态，用于统计和监控执行情况，所以正确地抛出异常，可以发现任务中的潜在问题，方便改进。

#### 9.TaskData类的两个泛型属性分别作为什么？

TaskData用于队列任务执行的传值。以及任务完成后返回结构。TaskParam和ResultData可通过泛型参数制定具体类型。

TaskParam执行参数，此数值必须由调用方设置。

ResultData是作为任务的返回结果。

#### 10.在哪里设置队列任务的限流类型？请详述不同限流类型的差异和使用场景。

在队列任务的TaskRunnerConfig中设置限流类型。

- RATE_LIMIT_LOCAL：本地进程限速，设置了进程内限速，没有设置任务名，这样所有的任务会共用一个限速器，导致会很卡。

- RATE_LIMIT_LOCAL_TASK：本地TASK限速，指定任务用指定限速器，但是没有对这个任务限速，可能导致服务器压力过大。

- RATE_LIMIT_LOCAL_TASK_TAG：本地TASK+TAG限速，指定任务用指定限速器，并且设置了流量限制TAG的大小。

- RATE_LIMIT_GLOBAL_TASK：全局TASK限速。

- RATE_LIMIT_GLOBAL_HOST：全局主机HOST限速，按照本机的外网IP限速。

- RATE_LIMIT_GLOBAL_TAG：全局TAG限速，按照流量限制TAG的大小限速。

- RATE_LIMIT_GLOBAL_TAG_HOST：全局TAG+HOST限速，按照本机的外网IP和流量限制TAG限速。

- RATE_LIMIT_GLOBAL_TASK_HOST：全局TASK+IP限速，所有任务按照本机的外网IP限速。

- RATE_LIMIT_GLOBAL_TASK_TAG：全局TASK+TAG限速，所有任务按照流量限制TAG的大小限速。

- RATE_LIMIT_GLOBAL_TASK_TAG_HOST：全局TASK+TAG+IP限速，所有任务按照本机的外网IP和流量限制TAG限速。

#### 11.uw-task什么情况下使用全局限流器？什么情况下使用本地限流器？全局限流器的限制是什么？

全局限流器使用Redis进行限速控制，本地限流器使用guava RateLimiter限速，从资源消耗上，全局限流器远大于本地限流器。

对于确定仅有单实例运行的项目，建议使用本地限流器，可以大幅度降低限流开销。

对于全局限速器，建议检测时间不要低于5S，减小网络io等开销。 

#### 12.uw-task的队列运行模式有哪几种？请详述不同队列类型的差异和使用场景。

- RUN_TYPE_LOCAL：本地运行，对于本地调用来说，不受任何流控限制。

- RUN_TYPE_GLOBAL：全局运行，会受到限速器的限制。

- RUN_TYPE_GLOBAL_RPC：全局运行RPC返回结果，对于RPC调用来说，不受任何流控限制。

- RUN_TYPE_AUTO_RPC：自动运行RPC返回结果，使用此模式，会自动选择本地和远程运行模式。

#### 13.uw-task的延时队列如何使用？

修改TaskRunnerConfig中的delayType属性为1，开启延时任务，这样在要发送任务到队列前选择合适的队列的时候，会获取到TTL队列的队列名，然后发送到该队列。