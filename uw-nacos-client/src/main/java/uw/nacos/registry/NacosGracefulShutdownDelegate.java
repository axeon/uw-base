package uw.nacos.registry;

import uw.nacos.discovery.NacosDiscoveryProperties;
import com.alibaba.nacos.common.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.jspecify.annotations.NonNull;

/**
 * Nacos 优雅停机委托。
 *
 * <p>监听 Spring 的 {@link ContextClosedEvent}（仅在当前上下文发布时生效，子上下文发布的忽略），
 * 在应用关闭时先调用 {@link NacosAutoServiceRegistration#stop()} 从 Nacos 摘除实例，
 * 再按 {@link NacosDiscoveryProperties#getGracefulShutdownWaitTime()} 等待一段时间，
 * 让上游流量切换完成后才继续关闭流程。{@link #supportsAsyncExecution()} 返回 false，
 * 确保优雅停机同步执行。
 */
public class NacosGracefulShutdownDelegate implements ApplicationListener<ContextClosedEvent>, ApplicationContextAware {

	/** 日志记录器。 */
	private static final Logger log = LoggerFactory.getLogger(NacosGracefulShutdownDelegate.class);

	/** 自动服务注册实例，用于在停机时摘除服务实例。 */
	private final NacosAutoServiceRegistration autoServiceRegistration;

	/** Nacos 服务发现配置属性，提供优雅停机宽限等待时间。 */
	private final NacosDiscoveryProperties nacosDiscoveryProperties;

	/** 注入的 Spring 应用上下文，用于判断 ContextClosedEvent 是否来自当前上下文。 */
	private ApplicationContext applicationContext;

	/**
	 * 构造 Nacos 优雅停机委托。
	 *
	 * @param autoServiceRegistration  Nacos 自动服务注册实例
	 * @param nacosDiscoveryProperties Nacos 服务发现配置属性
	 */
	public NacosGracefulShutdownDelegate(NacosAutoServiceRegistration autoServiceRegistration,
		NacosDiscoveryProperties nacosDiscoveryProperties) {
		this.autoServiceRegistration = autoServiceRegistration;
		this.nacosDiscoveryProperties = nacosDiscoveryProperties;
	}

	/**
	 * 注入当前 Spring 应用上下文。
	 * @param applicationContext Spring 应用上下文
	 * @throws BeansException 上下文注入异常
	 */
	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * 处理上下文关闭事件，仅当事件来自当前上下文时执行优雅停机，子上下文发布的事件忽略。
	 * @param event 上下文关闭事件
	 */
	@Override
	public void onApplicationEvent(@NonNull ContextClosedEvent event) {
		// should NOT be executed if ContextClosedEvent published by sub contexts
		if (!applicationContext.equals(event.getApplicationContext())) {
			log.debug("Nacos client graceful shutdown will NOT be executed "
					+ "for Spring context source: {}", event.getApplicationContext());
			return;
		}

		doGracefulShutdown();
	}

	/**
	 * 执行优雅停机：摘除服务实例并等待配置的宽限时间。
	 */
	private void doGracefulShutdown() {
		try {
			autoServiceRegistration.stop();
			Integer gracefulShutdownWaitTime = this.nacosDiscoveryProperties.getGracefulShutdownWaitTime();
			if (gracefulShutdownWaitTime != null && gracefulShutdownWaitTime > 0) {
				ThreadUtils.sleep(gracefulShutdownWaitTime);
			}

			log.info("Nacos client graceful shutdown has been executed successfully. " +
					"Graceful shutdown wait time is {}", gracefulShutdownWaitTime);
		}
		catch (Throwable t) {
			log.error("Error occurred while performing Nacos client graceful shutdown", t);
		}
	}

	/**
	 * 是否支持异步执行，固定返回 false 以确保优雅停机同步等待宽限时间结束。
	 * @return 固定返回 false
	 */
	@Override
	public boolean supportsAsyncExecution() {
		// need wait for graceful shutdown
		return false;
	}
}
