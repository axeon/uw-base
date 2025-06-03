[TOC]

## 简介

uw-task是一个分布式任务框架，通过uw-task可以快速构建分布式任务体系，支持定时任务和队列任务，同时支持任务运维监控和报警设置。

## 主要特性

1. 基于spring boot实现，依赖rabbitMQ,redis。
2. 完全分布式，支持混合云，可指定主机或指定集群运行。
3. 支持定时任务，并支持服务端动态配置。
4. 支持队列任务，支持各种流量控制，支持错误重试，并支持服务端动态配置。
5. 支持RPC风格调用，支持错误重试，并支持服务器端动态配置。
6. 支持多种规则的任务报警。

## maven引入

```
<dependency>
	<groupId>com.umtone</groupId>
	<artifactId>uw-task</artifactId>
	<version>${version}</version>
</dependency>
```

## 应用配置

直接使用spring boot的application.yml文件。

```yaml
#任务基础包名
uw:
  task:
    # 如果是任务执行主机，此配置应为true。
    enable-registry: true
    # 任务管理服务器地址
    task-center-host: http://localhost:8080
    # 任务项目，必须是包名前缀，用于扫描任务注册。
    task-project: com.demo.task
    # 运行目标，这是重要参数，用于识别任务执行集群，默认为default。
    run-target: default
    # croner线程数，默认在5个，建议按照实际croner任务数量*70%。
    croner-thread-num: 3
    # RPC最小线程数,用于执行RPC调用，如不使用rpc，建议设置为1，否则按照最大并发量*10%设置。
    task-rpc-min-thread-num: 1
    # RPC最大线程数,用于执行RPC调用，超过此线程数，将会导致阻塞。
    task-rpc-max-thread-num: 60
    # 本地队列任务运行最小线程数，用于运行本地队列任务，减少资源消耗。
    task-local-min-thread-num: 1
    # 本地队列任务运行最大线程数，用于运行本地队列任务，减少资源消耗。
    task-local-max-thread-num: 60
    # 队列任务重试延时毫秒数，默认2秒
    task-queue-retry-delay: 2000
    # rpc任务重试延时毫秒数，默认100毫秒
    task-rpc-retry-delay: 100
    
    # rabbitmq
    rabbitmq:
      host: 127.0.0.1
      port: 5672
      username: guest
      password: guest
      publisher-confirms: true
      virtual-host: /
  
    # redis 缓存
    redis:
      database: 0
      host: 127.0.0.1
      port: 6379
      password: password
      lettuce:
        pool:
          max-active: 20
          max-idle: 8
          max-wait: -1ms
          min-idle: 0
      timeout: 30s
```

## 定时任务配置

定时任务使用cron表达式来定时执行指定任务。

```
/**
 * 这是一个demo任务。
 * 定时任务需要继承TaskCroner。
 **/
@Component
public class DemoCronTask extends TaskCroner{

	/**
	 * 运行任务。
	 * 如果需要在日志中记录执行信息，请返回值中记录。
	 **/
	@Override
	public String runTask(TaskCronerLog taskCronerLog) throws TaskException {
		logger.info("just test for cron task!");
		return "";
	}

	/**
	 * 初始化定时任务配置。
	 * 在没有服务端配置的时候，默认使用此配置。
	 **/
	@Override
	public TaskCronerConfig initConfig() {
        TaskCronerConfig config = new TaskCronerConfig();
        config.setTaskName("测试定时任务");
        config.setTaskDesc("这是一个测试定时任务");
        //指定cron表达式
        config.setTaskCron("*/5 * * * * ?");
        //指定运行主机配置
        config.setRunTarget("");
        //指定运行模式
        config.setRunType(TaskCronerConfig.RUN_TYPE_SINGLETON);
        //报警信息：总失败率百分比数值
        config.setAlertFailRate(10);
        //程序失败率百分比数值
        config.setAlertFailProgramRate(10);
        //接口失败率百分比数值
        config.setAlertFailPartnerRate(10);
        //限速等待超时ms数
        config.setAlertWaitTimeout(10000);
        //运行超时ms数
        config.setAlertRunTimeout(1000);
        return config;
	}

	/**
	 * 初始化联系人信息。
	 * 用于在服务器端设置默认的报警通知信息。
	 */
	@Override
	public TaskContact initContact() {
		return new TaskContact("开发人员姓名", "手机号码", "邮箱地址", "微信", "im", “notifyUrl”，"备注");
	}

}

```

## 队列任务配置

- 实现TaskRunner接口的runTask方法。
- 对于接口异常，请抛出TaskPartnerException用于标识，可以引发重试。
- 对于执行结果，通过返回值来设置。

```
@Component
public class DemoTask extends TaskRunner<DemoTaskParam, String> {

	private static final Logger log = LoggerFactory.getLogger(DemoTask.class);

	
	@Override
	public String runTask(TaskData<DemoTaskParam, String> taskData) throws TaskException {
		log.info("这是一个DemoTask:{},{},{}", taskData.getTaskParam().getId(), taskData.getTaskParam().getName(),
				taskData.getTaskParam().getDate());
		return "ok";
	}
	
	/**
	 * 初始化队列任务配置。
	 * 在没有服务端配置的时候，默认使用此配置。
	 **/
	@Override
	public TaskRunnerConfig initConfig() {
		TaskRunnerConfig config = new TaskRunnerConfig();
		config.setTaskName("测试队列任务");
		config.setTaskDesc("测试队列任务描述");
		//设定限速类型
		config.setRateLimitType(TaskRunnerConfig.RATE_LIMIT_TYPE_NONE);
		//设定限速秒数
		config.setRateLimitTime(1);
		//设定限速次数
		config.setRateLimitValue(10);
		//设定限速超时等待时间
		config.setRateLimitWait(60);
		//设定因为对方接口错，重试的次数，默认为0
		config.setRetryTimesByPartner(0);
		//设定因为超过限速的错误，重试的次数，默认为0
		config.setRetryTimesByOverrated(0);
		//并发数，会开启几个并发进程处理任务。
		config.setConsumerNum(5);
		//预取数，大批量任务可以设置为5，一般任务建议设置为1
		config.setPrefetchNum(1);
		//总失败率百分比数值
		config.setAlertFailRate(10);
		//程序失败率百分比数值
		config.setAlertFailProgramRate(10);
	    //接口失败率百分比数值
		config.setAlertFailPartnerRate(10);
		//程序失败率百分比数值
		config.setAlertFailConfigRate(10);
		//队列排队超
		config.setAlertQueueOversize(1000);
		//队列排队超时ms数
		config.setAlertQueueTimeout(600000);
		//限速等待超时ms数
		config.setAlertWaitTimeout(10000);
		//运行超时ms数
		config.setAlertRunTimeout(1000);
		
		return config;
	}

	/**
	 * 初始化联系人信息。
	 * 用于在服务器端设置默认的报警通知信息。
	 */
	@Override
	public TaskContact initContact() {
		return new TaskContact("开发人员姓名", "手机号码", "邮箱地址", "微信", "im", “notifyUrl”，"备注");
	}
}

```

## 任务内异常处理

-
为了更好的支持监控，runTask的异常主要分为3类。分别是：TaskDataException数据异常；TaskPartnerException合作方异常；其他程序异常。其中限速超时异常由框架自动生成。程序异常、数据异常、接口方异常需要程序员来维护。
- TaskPartnerException 任务合作方异常，此异常会引发任务重试。
- TaskDataException 任务数据异常，此异常不会引发任务重试。
- 一般来说，尽量不要捕获异常，除非这个异常捕获后不影响任务的完整执行。不捕获的异常，框架会自动捕获为程序异常。

## 队列任务发布

1. 定义TaskFactory

```
@Autowired
private TaskFactory taskFactory;
```

2. 发送任务到队列，此操作为完全异步操作。

```
/**
 * 把任务发送到队列中。
 * 
 * @param target
 *            目标主机配置名，如果没有，则为空
 * @param taskData
 */
public void sendToQueue(TaskData<?, ?> taskData);
```

3. 本地运行队列。为了优化实时性高，且频繁的队列任务，可以优先在本地执行，减少**mq**压力，提升运行效率。当本地执行线程池满的时候，会直接转到队列执行。

```
    /**
     * 本地运行队列。
     * 为了优化实时性高，且频繁的队列任务，可以优先在本地执行，减少mq压力，提升运行效率。
     * 当本地执行线程池满的时候，会直接转到队列执行。
     *
     * @param taskData 任务数据
     */
    public void runQueue(final TaskData<?, ?> taskData) {
        taskQueueService.submit(new TaskQueueLocalExecutor(this, taskData));
    }
```

4. 本地执行任务。没有线程池支持，会导致阻塞。

```
    /**
     * 同步执行任务，没有线程池支持，会导致阻塞。
     * 在调用的时候，尤其要注意，taskData对象不可改变！
     *
     * @param taskData 任务数据
     * @return
     */
    @SuppressWarnings("unchecked")
    public <TP, RD> TaskData<TP, RD> runTaskLocal(final TaskData<TP, RD> taskData) {
        taskData.setId(globalSequenceManager.nextId("task_runner_log"));
        taskData.setQueueDate(SystemClock.nowDate());
        // 当自动RPC，并且本地有runner，而且target匹配的时候，运行在本地模式下。
        if (taskData.getRunType() == TaskData.RUN_TYPE_AUTO_RPC && TaskMetaInfoManager.checkRunnerRunLocal(taskData)) {
            // 启动本地运行模式。
            taskData.setRunType(TaskData.RUN_TYPE_LOCAL);
        }
        if (taskData.getRunType() == TaskData.RUN_TYPE_LOCAL) {
            taskRunnerContainer.process(taskData);
            return taskData;
        } else {
            throw new TaskRuntimeException(taskData.getClass().getName() + " is not a local task! ");
        }
    }
```

5. 同步执行任务
   运行期，程序根据runType判断，并结合任务代码是否在本地的判定，决定是运行在本地还是远程。
   一般来说，程序的默认runType=RUN_TYPE_AUTO_RPC，此时是自动判定默认。
   也可以指定RUN_TYPE_GLOBAL_RPC和RUN_TYPE_LOCAL。

```
	/**
	 * 同步执行任务，可能会导致阻塞。
	 *
	 * @param runTarget
	 *            目标主机配置名，如果没有，则为空
	 * @param taskData
	 *            任务数据
	 * @return
	 */
	public <TP, RD> TaskData<TP, RD> runTask(final TaskData<TP, RD> taskData,final TypeReference<TaskData<TP, RD>> typeRef);
```

6. 异步执行任务

```
	/**
	 * 远程运行任务，并返回future<TaskData<?,?>>。 如果需要获取数据，可以使用futrue.get()来获取。
	 * 此方法要谨慎使用，因为task存在限速，大并发下可能会导致线程数超。
	 * @param runTarget
	 *            目标主机配置名，如果没有，则为空
	 * @param taskData
	 *            任务数据
	 * @return
	 */
	public <TP, RD> Future<TaskData<TP, RD>> runTaskAsync(final TaskData<TP, RD> taskData,
			final TypeReference<TaskData<TP, RD>> typeRef);
```

## TaskData说明

TaskData是分发任务传递参数和返回

```
/**
 * TaskData用于任务执行的传值，以为任务完成后返回结构。
 * TaskParam和ResultData可通过泛型参数制定具体类型。
 * TP,TD应和TaskRunner的泛型参数完全一致，否则会导致运行时出错。
 *
 */
public class TaskData<TP,TD> implements Serializable {

    /**
     * 任务状态:未设置
     */
    public static final int STATUS_UNKNOW = 0;
    
    /**
     * 任务状态:成功
     */
    public static final int STATUS_SUCCESS = 1;
    
    /**
     * 任务状态:程序错误
     */
    public static final int STATUS_FAIL_PROGRAM = 2;
    
    /**
     * 任务状态:配置错误，如超过流量限制
     */
    public static final int STATUS_FAIL_CONFIG = 3;
    
    /**
     * 任务状态:第三方接口错误
     */
    public static final int STATUS_FAIL_PARTNER = 4;
    
    /**
     * 运行模式：本地运行
     */
    public static final int RUN_TYPE_LOCAL = 1;
    
    /**
     * 运行模式：全局运行
     */
    public static final int RUN_TYPE_GLOBAL = 3;	
    
    /**
     * 运行模式：全局运行RPC返回结果
     */
    public static final int RUN_TYPE_GLOBAL_RPC = 5;	

    /**
     * id，此序列值由框架自动生成，无需手工设置。
     */
    private long id;
    
    /**
     * 关联TAG，由调用方设定，用于第三方统计信息。
     */
    private String refTag;
    
    /**
     * 任务延迟毫秒数。一般这个时间不宜太长，大多数情况下不要超过60秒。
     */
    private long taskDelay;
    
    /**
     * 关联id，由调用方根据需要设置，用于第三方统计信息。
     */
    private long refId;

    /**
     * 关联子id，由调用方根据需要设置，用于第三方统计信息。
     */
    private long refSubId;

    /**
     * 关联对象，此对象不存入数据库，但可以通过Listener来访问。
     */
    private Object refObject;

    /**
     * 流量限制TAG。
     */
    private String rateLimitTag;
    
    /**
     * 需要执行的类名，此数值必须由调用方设置。
     */
    private String taskClass = "";

    /**
     * 任务标签，用于细分任务队列，支持多实例运行。
     */
    private String taskTag = "";
    
    /**
     * 执行参数，此数值必须有调用方设置。
     */
    private TP taskParam;

    /**
     * 任务运行类型。由框架设置，无需手工设置。
     */
    private int runType;

    /**
     * 指定运行目标。
     */
    private String runTarget = "";

    /**
     * 任务运行时主机IP，此信息由框架自动设置。
     */
    private String hostIp;

    /**
     * 任务运行时主机ID（可能为docker的ContainerID），此信息由框架自动设置。
     */
    private String hostId;

    /**
     * 进入队列时间，此信息由框架自动设置。
     */
    private Date queueDate;

    /**
     * 开始消费时间，此信息由框架自动设置。
     */
    private Date consumeDate;

    /**
     * 开始运行时间，此信息由框架自动设置。
     */
    private Date runDate;

    /**
     * 运行结束日期，此信息由框架自动设置。
     */
    private Date finishDate;

    /**
     * 执行信息，用于存储框架自动设置。
     */
    private RD resultData;

    /**
     * 出错信息
     */
    private String errorInfo;

    /**
     * 已经执行的次数，此信息由框架自动设置。
     */
    private int ranTimes;

    /**
     * 执行状态，此信息由框架自动设置。
     */
    private int status;


}

```

## 流量控制功能

在TaskRunnerConfig中定义了以下几种流量限制方式。对于全局限速器，建议检测时间不要低于5S。

```
	/**
	 * 限速类型：不限速
	 */
	public static final int RATE_LIMIT_TYPE_NONE = 0;
	
	/**
	 * 限速类型：基于当前进程的限速，所有任务共用一个进程限速器。
	 */
	public static final int RATE_LIMIT_TYPE_PROCESS = 1;
	
	/**
	 * 限速类型：基于当前主机IP限速，所有任务共用一个IP的限速器。
	 */
	public static final int RATE_LIMIT_TYPE_IP = 2;

	/**
	 * 限速类型：基于TaskData的RateLimitTag（推荐设定为接口配置ID）限速
	 */
	public static final int RATE_LIMIT_TYPE_TAG = 3;
	
	/**
	 * 限速类型：基于TaskName限速，基于当前任务限速。
	 */
	public static final int RATE_LIMIT_TYPE_TASK = 5;
	
	/**
	 * 限速类型：进程内基于当前任务的限速
	 */
	public static final int RATE_LIMIT_TYPE_TASK_PROCESS = 6;

	/**
	 * 限速类型：基于当前任务的当前主机IP限速
	 */
	public static final int RATE_LIMIT_TYPE_TASK_IP = 7;

	/**
	 * 限速类型：根据当前任务的TaskData的RateLimitTag（推荐设定为接口配置ID）限速
	 */
	public static final int RATE_LIMIT_TYPE_TASK_TAG = 8;
	
	/**
	 * 限速类型：根据当前IP和进程限速
	 */
	public static final int RATE_LIMIT_TYPE_TAG_PROCESS = 9;
	
	/**
	 * 限速类型：根据当前IP和TaskData的RateLimitTag（推荐设定为接口配置ID）限速
	 */
	public static final int RATE_LIMIT_TYPE_TAG_IP = 10;

```

流量限制的参数设定：

```
    /**
     * 详见流量限制类型说明。
     */
    private int rateLimitType = 1;

    /**
     * 流量限定数值，默认为10次
     */
    private int rateLimitValue = 10;

    /**
     * 流量限定时间(S)，默认为1秒
     */
    private int rateLimitTime = 1;

    /**
     * 当发生流量限制时，等待的秒数，默认60秒
     */
    private int rateLimitWait = 60;
    
    /**
     * 超过流量限制重试次数，默认不再重试，放弃任务。
     */
    private int retryTimesByOverrated = 0;

```

## 多实例运行

- 有时候会存在一套定时任务/队列任务，存在多个并发运行任务实例的情况。此功能需要经过服务器端多配置来实现。
- TaskCroner通过配置中的TaskParam参数来指标识多实例。在实际使用中，可以使用空值（默认值）来处理绝大多数请求，特定用户使用特定ID作为TaskParam数值。
- TaskRunner通过TaskData/TaskRunnerConfig中的TaskTag来标识多实例。发送任务的时候，就需要指定TaskTag/RunTarget。
- 如果指定的TaskRunner的TaskTag&RunTarget无法匹配到指定服务器端配置，框架会宽松匹配最合适的配置。

## 运行目标RunTarget

- 运行目标是非常重要的参数，它和taskProject一起决定了任务配置的同步联系。

- 任务可以指定运行目标，此运行目标通过服务器端配置来实现。

## 常见问题

**任务不能注册，无法启动任务。**

1.任务上有没有设置@Componet注解？uw-task的任务通过Spring的@Componet注解扫描识别并注入系统。

**尼玛队列堵成狗，队列任务根本就没按照限速执行。**

是不是不看文档？限速类型设定为“进程内限速”了？这样所有的任务会共用一个限速器，不卡死你才怪。认真阅读文档，选择合理的限速类型！
 
 **关于uw-task的延时队列任务**
 当前uw-task的延时队列任务通过死信队列实现，所以存在长延时任务会阻塞短延时任务问题，需要谨慎使用。
 一般情况下，并不推荐使用mq的延时队列任务，这不是一个节省资源的方案。
 小负载情况可以直接轮询数据库；大负载情况可使用uw-cache:GlobalSortedSet，均可有效降低资源消耗。