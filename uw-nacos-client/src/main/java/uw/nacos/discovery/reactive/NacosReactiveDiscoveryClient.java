package uw.nacos.discovery.reactive;

import java.util.function.Function;

import uw.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;

/**
 * 基于 Nacos 的反应式 {@link ReactiveDiscoveryClient} 实现。
 *
 * <p>实现 Spring Cloud Commons 的 {@link ReactiveDiscoveryClient} 接口，供 WebFlux/响应式场景
 * （如 uw-gateway）使用。查询逻辑委托给 {@link NacosServiceDiscovery}（阻塞调用），通过
 * {@code boundedElastic} 调度器在独立线程执行以避免阻塞响应式事件循环。
 */
public class NacosReactiveDiscoveryClient implements ReactiveDiscoveryClient {

	/** 日志记录器。 */
	private static final Logger log = LoggerFactory
			.getLogger(NacosReactiveDiscoveryClient.class);

	/** 委托的 Nacos 服务发现查询封装（阻塞调用），通过 boundedElastic 调度器在独立线程执行。 */
	private NacosServiceDiscovery serviceDiscovery;

	/** 按 serviceId 加载实例的映射函数（构造期固化，避免每次查询分配新 lambda）。 */
	private final Function<String, Publisher<ServiceInstance>> loadInstancesFunction;

	/**
	 * 构造反应式 Nacos 发现客户端。
	 *
	 * @param nacosServiceDiscovery 委托的 Nacos 服务发现查询封装
	 */
	public NacosReactiveDiscoveryClient(NacosServiceDiscovery nacosServiceDiscovery) {
		this.serviceDiscovery = nacosServiceDiscovery;
		this.loadInstancesFunction = serviceId -> {
			try {
				return Mono.justOrEmpty(serviceDiscovery.getInstances(serviceId))
						.flatMapMany(Flux::fromIterable);
			}
			catch (NacosException e) {
				log.error("get service instance[{}] from nacos error!", serviceId, e);
				return Flux.empty();
			}
		};
	}

	/**
	 * 返回该反应式发现客户端的描述信息。
	 * @return 客户端描述字符串
	 */
	@Override
	public String description() {
		return "Spring Cloud Nacos Reactive Discovery Client";
	}

	/**
	 * 异步查询指定服务的全部健康实例，订阅时在 {@code boundedElastic} 调度器线程执行以避免阻塞事件循环。
	 * @param serviceId 服务 id
	 * @return 服务实例响应流
	 */
	@Override
	public Flux<ServiceInstance> getInstances(String serviceId) {
		return Mono.justOrEmpty(serviceId).flatMapMany(loadInstancesFunction)
				.subscribeOn(Schedulers.boundedElastic());
	}

	/**
	 * 异步查询全部已注册服务名列表，查询失败时降级为空流。
	 * @return 服务名响应流
	 */
	@Override
	public Flux<String> getServices() {
		return Flux.defer(() -> {
			try {
				return Mono.justOrEmpty(serviceDiscovery.getServices())
						.flatMapMany(Flux::fromIterable);
			}
			catch (Exception e) {
				log.error("get services from nacos server fail,", e);
				return Flux.empty();
			}
		}).subscribeOn(Schedulers.boundedElastic());
	}

}
