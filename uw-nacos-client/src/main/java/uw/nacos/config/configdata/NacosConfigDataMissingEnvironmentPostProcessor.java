package uw.nacos.config.configdata;

import uw.nacos.config.NacosPropertiesPrefixer;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;
import org.springframework.core.env.Environment;

import static uw.nacos.config.configdata.NacosConfigDataLocationResolver.PREFIX;

/**
 * Nacos ConfigData 缺失检测后处理器。
 *
 * <p>当启用了 Nacos 配置（{@code spring.cloud.nacos.config.enabled}，默认 true）且 import-check
 * 开启（{@code spring.cloud.nacos.config.import-check.enabled}，默认 true）时，校验
 * {@code spring.config.import} 中是否声明了 {@code nacos:} 导入；缺失则通过内置
 * {@link ImportExceptionFailureAnalyzer} 给出修复建议。
 *
 * @since 2021.0.1.0
 */
public class NacosConfigDataMissingEnvironmentPostProcessor
		extends ConfigDataMissingEnvironmentPostProcessor {

	/**
	 * 执行顺序：晚于 {@link ConfigDataEnvironmentPostProcessor}。
	 */
	private static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1000;

	/**
	 * @return 后处理器执行顺序
	 */
	@Override
	public int getOrder() {
		return ORDER;
	}

	/**
	 * 仅当 nacos config 启用且 import-check 启用时才需要校验。
	 * @param environment 当前环境
	 * @return 是否需要处理
	 */
	@Override
	protected boolean shouldProcessEnvironment(Environment environment) {
		String configPrefix = NacosPropertiesPrefixer.DEFAULT_PREFIX + ".config";

		boolean configEnabled = environment.getProperty(
				configPrefix + ".enabled", Boolean.class, true);
		boolean importCheckEnabled = environment.getProperty(
				configPrefix + ".import-check.enabled", Boolean.class,
				true);
		return configEnabled && importCheckEnabled;
	}

	/**
	 * @return 期望的 ConfigData location 前缀（{@code nacos:}）
	 */
	@Override
	protected String getPrefix() {
		return PREFIX;
	}

	/**
	 * 缺失导入异常的失败分析器，输出修复指引文案。
	 */
	static class ImportExceptionFailureAnalyzer
			extends AbstractFailureAnalyzer<ImportException> {

		/**
		 * 根据异常类型生成失败描述与修复指引。
		 * @param rootFailure 根异常
		 * @param cause ImportException
		 * @return FailureAnalysis
		 */
		@Override
		protected FailureAnalysis analyze(Throwable rootFailure, ImportException cause) {
			String description;
			if (cause.missingPrefix) {
				description = "The spring.config.import property is missing a " + PREFIX
						+ " entry";
			}
			else {
				description = "No spring.config.import property has been defined";
			}
			String action = "Add a spring.config.import=nacos: property to your configuration.\n"
					+ "\tIf configuration is not required add spring.config.import=optional:nacos: instead.\n"
					+ "\tTo disable this check, set spring.cloud.nacos.config.import-check.enabled=false.";
			return new FailureAnalysis(description, action, cause);
		}

	}

}
