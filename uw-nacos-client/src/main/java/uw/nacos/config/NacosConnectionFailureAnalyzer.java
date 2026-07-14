package uw.nacos.config;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Nacos 连接失败的失败分析器。
 *
 * <p>启动期创建 {@code ConfigService} 时若连不上 Nacos Server（抛
 * {@link NacosConnectionFailureException}），输出可读的失败描述与修复指引，避免原始
 * nacos-client 异常堆栈直接呈现给用户。描述使用固定文案，不拼接 {@code cause.getMessage()}，
 * 以免泄露 nacos-client 内部关键字。
 */
public class NacosConnectionFailureAnalyzer
		extends AbstractFailureAnalyzer<NacosConnectionFailureException> {

	/**
	 * 输出可读的连接失败描述与修复指引（固定文案，不拼接 cause.getMessage 以免泄露 nacos-client 内部关键字）。
	 * @param rootFailure 根异常
	 * @param cause Nacos 连接失败异常
	 * @return 失败分析
	 */
	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			NacosConnectionFailureException cause) {
		return new FailureAnalysis(
				"无法连接到 Nacos Server: " + cause.getServerAddr(),
				"请检查 Nacos Server 地址是否正确、网络是否可达、鉴权信息（username/password 或 accessKey/secretKey）是否匹配。",
				cause);
	}

}
