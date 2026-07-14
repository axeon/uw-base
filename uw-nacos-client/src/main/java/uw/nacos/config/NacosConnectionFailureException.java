package uw.nacos.config;

/**
 * 连接 Nacos Server 失败时抛出的运行时异常。
 *
 * <p>由 {@link NacosConfigManager} 在创建 {@code ConfigService} 失败时抛出，携带目标 server 地址，
 * 便于上层定位连接问题。
 *
 */
public class NacosConnectionFailureException extends RuntimeException {

	/**
	 * 连接失败的 Nacos Server 地址。
	 */
	private final String serverAddr;

	/**
	 * @param serverAddr 连接失败的 Nacos Server 地址
	 * @param message 异常信息
	 */
	public NacosConnectionFailureException(String serverAddr, String message) {
		super(message);
		this.serverAddr = serverAddr;
	}

	/**
	 * @param serverAddr 连接失败的 Nacos Server 地址
	 * @param message 异常信息
	 * @param cause 原始异常
	 */
	public NacosConnectionFailureException(String serverAddr, String message,
			Throwable cause) {
		super(message, cause);
		this.serverAddr = serverAddr;
	}

	/**
	 * @return 连接失败的 Nacos Server 地址
	 */
	public String getServerAddr() {
		return serverAddr;
	}

}
