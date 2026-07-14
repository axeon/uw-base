package uw.nacos.config.configdata;

import java.util.Objects;

import uw.nacos.config.NacosConfigProperties;

import org.springframework.boot.context.config.ConfigDataResource;
import org.springframework.boot.context.config.Profiles;

/**
 * Nacos ConfigData 资源描述，继承 Spring Boot 的 {@link ConfigDataResource}。
 *
 * <p>描述一份待加载的 Nacos 配置：所属 {@link NacosConfigProperties}、是否 optional、激活的
 * profiles，以及具体的 {@link NacosItemConfig}（group/dataId/suffix/刷新/优先级）。由
 * {@link NacosConfigDataLocationResolver} 产出，供 {@link NacosConfigDataLoader} 加载。
 *
 * @since 2021.0.1.0
 */
public class NacosConfigDataResource extends ConfigDataResource {

	/**
	 * 关联的 Nacos 配置属性。
	 */
	private final NacosConfigProperties properties;

	/**
	 * 是否可选（缺失不报错）。
	 */
	private final boolean optional;

	/**
	 * 激活的 profiles。
	 */
	private final Profiles profiles;

	/**
	 * 单条 Nacos 配置项描述。
	 */
	private final NacosItemConfig config;

	/**
	 * @param properties Nacos 配置属性
	 * @param optional 是否可选（缺失不报错）
	 * @param profiles 激活的 profiles
	 * @param config 单条 Nacos 配置项描述
	 */
	public NacosConfigDataResource(NacosConfigProperties properties, boolean optional,
			Profiles profiles, NacosItemConfig config) {
		this.properties = properties;
		this.optional = optional;
		this.profiles = profiles;
		this.config = config;
	}

	/**
	 * @return 是否可选
	 */
	public boolean isOptional() {
		return this.optional;
	}

	/**
	 * @return 单条 Nacos 配置项描述
	 */
	public NacosItemConfig getConfig() {
		return config;
	}

	/**
	 * 基于 properties/optional/profiles/config 判等。
	 * @param o 另一对象
	 * @return 是否相等
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		NacosConfigDataResource that = (NacosConfigDataResource) o;
		return optional == that.optional && Objects.equals(properties, that.properties)
				&& Objects.equals(profiles, that.profiles)
				&& Objects.equals(config, that.config);
	}

	/**
	 * 基于 properties/optional/profiles/config 计算哈希。
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		return Objects.hash(properties, optional, profiles, config);
	}

	/**
	 * 摘要输出。
	 * @return 摘要字符串
	 */
	@Override
	public String toString() {
		return "NacosConfigDataResource{" + "properties=" + properties + ", optional="
				+ optional + ", profiles=" + profiles + ", config=" + config + '}';
	}

	/**
	 * 单条 Nacos 配置项描述：group、dataId、文件后缀、是否刷新、优先级。
	 */
	public static class NacosItemConfig {
		/**
		 * 配置 group。
		 */
		private String group;
		/**
		 * 配置 dataId。
		 */
		private String dataId;
		/**
		 * 文件后缀。
		 */
		private String suffix;
		/**
		 * 是否启用刷新。
		 */
		private boolean refreshEnabled;
		/**
		 * 优先级（local/remote）。
		 */
		private String preference;

		/**
		 * 默认构造。
		 */
		public NacosItemConfig() {
		}

		/**
		 * @param group 配置 group
		 * @return 当前对象（链式设置）
		 */
		public NacosItemConfig setGroup(String group) {
			this.group = group;
			return this;
		}

		/**
		 * @param dataId 配置 dataId
		 * @return 当前对象（链式设置）
		 */
		public NacosItemConfig setDataId(String dataId) {
			this.dataId = dataId;
			return this;
		}

		/**
		 * @param suffix 文件后缀
		 * @return 当前对象（链式设置）
		 */
		public NacosItemConfig setSuffix(String suffix) {
			this.suffix = suffix;
			return this;
		}

		/**
		 * @param refreshEnabled 是否启用刷新
		 * @return 当前对象（链式设置）
		 */
		public NacosItemConfig setRefreshEnabled(boolean refreshEnabled) {
			this.refreshEnabled = refreshEnabled;
			return this;
		}

		/**
		 * @param preference 优先级（local/remote）
		 * @return 当前对象（链式设置）
		 */
		public NacosItemConfig setPreference(String preference) {
			this.preference = preference;
			return this;
		}

		/**
		 * @return 配置 group
		 */
		public String getGroup() {
			return group;
		}

		/**
		 * @return 配置 dataId
		 */
		public String getDataId() {
			return dataId;
		}

		/**
		 * @return 文件后缀
		 */
		public String getSuffix() {
			return suffix;
		}

		/**
		 * @return 是否启用刷新
		 */
		public boolean isRefreshEnabled() {
			return refreshEnabled;
		}

		/**
		 * @return 优先级（local/remote）
		 */
		public String getPreference() {
			return preference;
		}

		/**
		 * 基于 group/dataId/suffix/refreshEnabled/preference 判等。
		 * @param o 另一对象
		 * @return 是否相等
		 */
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			NacosItemConfig that = (NacosItemConfig) o;
			return refreshEnabled == that.refreshEnabled
					&& Objects.equals(group, that.group)
					&& Objects.equals(dataId, that.dataId)
					&& Objects.equals(suffix, that.suffix)
					&& Objects.equals(preference, that.preference);
		}

		/**
		 * 基于 group/dataId/suffix/refreshEnabled/preference 计算哈希。
		 * @return 哈希值
		 */
		@Override
		public int hashCode() {
			return Objects.hash(group, dataId, suffix, refreshEnabled, preference);
		}

		/**
		 * 摘要输出。
		 * @return 摘要字符串
		 */
		@Override
		public String toString() {
			return "NacosItemConfig{" + "group='" + group + '\'' + ", dataId='" + dataId
					+ '\'' + ", suffix='" + suffix + '\'' + ", refreshEnabled="
					+ refreshEnabled + ", preference=" + preference + '}';
		}
	}

}
