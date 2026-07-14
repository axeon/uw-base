package uw.nacos.discovery;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.commons.util.InetUtilsProperties;

/**
 * IPv6 地址探测工具。
 *
 * <p>遍历本机网卡，按 {@link InetUtilsProperties} 中的偏好网络与忽略接口规则筛选出首个可用的
 * 非回环、非链路本地、非唯一本地地址的 IPv6 地址，供 Nacos 服务注册时填充 IPv6 元数据使用。
 * 偏好网络与忽略接口的正则在构造期一次性预编译，避免每个候选地址重复编译。
 */
public class InetIPv6Utils {

	/** 日志记录器。 */
	private final static Log log = LogFactory.getLog(InetIPv6Utils.class);

	/** Spring Cloud 通用网络工具配置，提供偏好网络、忽略接口等筛选规则。 */
	private final InetUtilsProperties properties;

	/** 偏好网络正则列表，构造期一次性预编译，匹配地址 hostAddress 时使用。 */
	private final List<Pattern> preferredNetworkPatterns;

	/** 忽略网卡正则列表，构造期一次性预编译，匹配网卡名称时使用。 */
	private final List<Pattern> ignoredInterfacePatterns;

	/**
	 * 构造 IPv6 地址探测工具，并在构造期预编译偏好网络与忽略接口的正则。
	 *
	 * @param properties Spring Cloud 通用网络工具配置
	 */
	public InetIPv6Utils(final InetUtilsProperties properties) {
		this.properties = properties;
		this.preferredNetworkPatterns = compilePatterns(properties.getPreferredNetworks());
		this.ignoredInterfacePatterns = compilePatterns(properties.getIgnoredInterfaces());
	}

	/**
	 * 将正则字符串列表批量预编译为 {@link Pattern} 列表，空入参返回空列表。
	 *
	 * @param regexes 正则字符串列表
	 * @return 已编译的正则列表
	 */
	private static List<Pattern> compilePatterns(List<String> regexes) {
		if (regexes == null || regexes.isEmpty()) {
			return Collections.emptyList();
		}
		List<Pattern> patterns = new ArrayList<>(regexes.size());
		for (String regex : regexes) {
			patterns.add(Pattern.compile(regex));
		}
		return patterns;
	}

	/**
	 * 遍历本机网卡查找首个可用的 IPv6 地址。
	 * <p>过滤回环、链路本地、任意本地、站点本地、唯一本地地址，并应用偏好网络与忽略网卡规则。
	 * @return 首个可用 IPv6 地址，未找到或发生 IO 异常时返回 null
	 */
	private InetAddress findFirstValidIPv6Address() {
		InetAddress address = null;

		try {
			for (Enumeration<NetworkInterface> nics = NetworkInterface
					.getNetworkInterfaces(); nics.hasMoreElements(); ) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp() && !ifc.isVirtual() && !ifc.isLoopback()) {
					if (address != null) {
						break;
					}

					if (!ignoreInterface(ifc.getDisplayName())) {
						for (Enumeration<InetAddress> addrs = ifc
								.getInetAddresses(); addrs.hasMoreElements(); ) {
							InetAddress inetAddress = addrs.nextElement();
							if (inetAddress instanceof Inet6Address
									// filter ::1
									&& !inetAddress.isLoopbackAddress()
									// filter fe80::/10
									&& !inetAddress.isLinkLocalAddress()
									// filter ::/128
									&& !inetAddress.isAnyLocalAddress()
									// filter fec0::/10,which was discarded, but some
									// address may be deployed.
									&& !inetAddress.isSiteLocalAddress()
									// filter fd00::/8
									&& !isUniqueLocalAddress(inetAddress)
									&& isPreferredAddress(inetAddress)) {
								if (log.isTraceEnabled()) {
									log.trace("Found non-loopback interface: "
											+ ifc.getDisplayName());
								}
								address = inetAddress;
								break;
							}
						}
					}
				}
			}
		}
		catch (IOException e) {
			log.error("Cannot get first non-loopback address", e);
		}
		return address;
	}

	/**
	 * 查找本机首个可用 IPv6 地址，并规范化为加方括号的形式（去除网卡后缀）。
	 *
	 * @return 规范化后的 IPv6 地址字符串（如 {@code [2408:400a:...]}），无可用地址时返回 null
	 */
	public String findIPv6Address() {
		InetAddress address = findFirstValidIPv6Address();
		if (address == null) {
			return null;
		}
		return normalizeIPv6(address.getHostAddress());
	}

	/**
	 * 将 IPv6 地址规范化为加方括号的形式，并去除网卡后缀（如 {@code %eth0}）。
	 *
	 * @param ip 原始 IPv6 地址字符串
	 * @return 加方括号且去除后缀的 IPv6 地址字符串
	 */
	private String normalizeIPv6(String ip) {
		// Remove the suffix of network card in IPv6 address, such as
		// 2408:400a:8c:5400:6578:5c42:77b1:bc5d%eth0
		int idx = ip.indexOf("%");
		return idx != -1 ? "[" + ip.substring(0, idx) + "]" : "[" + ip + "]";
	}

	/**
	 * 判断给定地址是否符合偏好网络规则。
	 * <p>启用 {@code useOnlySiteLocalInterfaces} 时仅接受站点本地地址；否则在偏好网络列表为空时全部接受，
	 * 否则要求 hostAddress 匹配任一偏好正则或以其前缀开头。
	 * @param address 待判定的网络地址
	 * @return 符合偏好网络规则返回 true，否则返回 false
	 */
	private boolean isPreferredAddress(InetAddress address) {
		if (this.properties.isUseOnlySiteLocalInterfaces()) {
			final boolean siteLocalAddress = address.isSiteLocalAddress();
			if (!siteLocalAddress && log.isTraceEnabled()) {
				log.trace("Ignoring address" + address.getHostAddress());
			}
			return siteLocalAddress;
		}
		if (preferredNetworkPatterns.isEmpty()) {
			return true;
		}
		final String hostAddress = address.getHostAddress();
		for (Pattern pattern : preferredNetworkPatterns) {
			if (pattern.matcher(hostAddress).matches()
					|| hostAddress.startsWith(pattern.pattern())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断指定网卡名称是否匹配任一忽略接口正则。
	 *
	 * @param interfaceName 网卡名称
	 * @return 命中忽略规则返回 true，否则返回 false
	 */
	private boolean ignoreInterface(String interfaceName) {
		for (Pattern pattern : ignoredInterfacePatterns) {
			if (pattern.matcher(interfaceName).matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判断该地址是否属于 IPv6 唯一本地地址（Unique Local Address，{@code fd00::/8}）。
	 *
	 * @param inetAddress {@link InetAddress}
	 * @return 是唯一本地地址返回 true，否则返回 false
	 */
	private boolean isUniqueLocalAddress(InetAddress inetAddress) {
		byte[] ip = inetAddress.getAddress();
		return (ip[0] & 0xff) == 0xfd;
	}

}
