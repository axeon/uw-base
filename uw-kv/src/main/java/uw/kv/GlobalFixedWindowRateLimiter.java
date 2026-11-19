package uw.kv;

import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.BaseRedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.CommandOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.ProtocolKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;
import uw.common.util.SystemClock;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于 Redis 的全局固定窗口限速器，限速器家族的全局（Global）形态，亦是 INCREX 命令族的宿主。
 *
 * <p>在多个实例之间共享同一限流配额。窗口边界对齐到墙钟网格（{@code windowEnd = (now/windowMillis+1)*windowMillis}），
 * 与本地形态 {@link uw.common.util.FixedWindowRateLimiter} 的窗口边界语义一致——同配置下本地/全局行为可互推。</p>
 *
 * <h3>主路径：INCREX（Redis Open Source 8.8+）</h3>
 * <pre>
 * INCREX uw-rate:{name} BYINT permits PXAT windowEnd   // 单命令原子：累加 + 幂等重设窗口终点
 * </pre>
 * 返回累加后计数，超上限（&gt; requests）即拒绝，剩余等待毫秒由<b>客户端本地计算</b>
 * （windowEnd - now，窗口终点已知，无需额外 PTTL 往返）。PXAT 幂等重设过期时刻，无"首次请求判定"分支。
 *
 * <h3>降级路径：Lua fallback</h3>
 * INCREX 能力在<b>启动时探测一次</b>（构造器直接试命令）：支持则固定走 INCREX 主路径，不支持
 * （版本 &lt; 8.8，特征为 unknown command）则固定走等价 Lua（{@code INCRBY + PEXPIREAT 窗口终点}，
 * 同样单往返原子）。无运行期冷却/重探——Redis 版本在应用运行期不会变化，升级伴随滚动重启；
 * 启动探测漏网时（启动时 Redis 不可达的乐观默认）运行期遇 unknown command 一次性翻转 Lua。
 * <p>
 * <b>降级目标必须是 Lua 而非裸 INCRBY</b>：裸 INCRBY 无 TTL = 窗口永不翻转 = 一次触顶永久限流，
 * 属功能错误——与 {@link GlobalCounter} 的降级语义（裸 INCRBY，计数保全可接受）有本质区别。
 * 连接类故障（非 unknown command）不触发降级，走放行语义（见下）。
 *
 * <h3>异常与放行语义</h3>
 * Redis 连接异常时返回 0（放行），优先保证调用方可用性，等同限流降级。
 *
 * <h3>语义偏差</h3>
 * 固定窗口在窗口边界处存在最多 2 倍突发；超限的申请同样计入窗口计数（计数只增不减，不影响剩余时间判定）。
 *
 * <h3>维度能力</h3>
 * 单维度（许可）走 INCREX 单命令主路径；双维度（许可+权重）走 Lua 双 key 原子路径
 * （{@code uw-rate:{name}:p/:w}），均支持两阶段（入口不预占权重、{@link #addWeight} 事后回填），
 * 与本地形态的双维度语义对齐。
 *
 * <h3>name 约定</h3>
 * 调用方应在 name 中编码自身域前缀（如 task 域传 {@code uw-task-xxx}），避免不同域共享配额池。
 *
 * <h3>INCREX 命令族宿主</h3>
 * INCREX（单命令原子完成 INCRBY+PEXPIRE/PXAT，8.8+ 新命令，Spring/Lettuce 均未收录）的发送与解析实现
 * 位于本类（{@link #increx}/{@link #isUnknownCommandError}），供 {@link GlobalCounter}（TTL 计数）复用。
 *
 * @author axeon
 */
public class GlobalFixedWindowRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(GlobalFixedWindowRateLimiter.class);

    /**
     * redis key 前缀。
     */
    private static final String REDIS_PREFIX = "uw-rate:";

    /**
     * INCREX 命令的整型增量选项字面量（BYINT）。
     */
    static final byte[] BYINT_BYTES = "BYINT".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令的毫秒级过期选项字面量（PX）。
     */
    static final byte[] PX_BYTES = "PX".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令的绝对过期时间戳选项字面量（PXAT，Unix 毫秒）。
     */
    static final byte[] PXAT_BYTES = "PXAT".getBytes(StandardCharsets.UTF_8);

    /**
     * INCREX 命令关键字（Spring Data Redis / Lettuce 均未收录 8.8+ 新命令，自定义 ProtocolKeyword 发送）。
     */
    private static final ProtocolKeyword INCREX_KEYWORD = new ProtocolKeyword() {
        @Override
        public byte[] getBytes() {
            return "INCREX".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toString() {
            return "INCREX";
        }
    };

    /**
     * 双维度 Lua 脚本：双 key 原子累加 + 幂等重设窗口终点 + 双维度判定。
     * <p>
     * KEYS[1]=许可 key，KEYS[2]=权重 key，ARGV[1]=permits，ARGV[2]=weight，ARGV[3]=windowEnd（Unix 毫秒），
     * ARGV[4]=limitPermits，ARGV[5]=limitWeight。返回 1=超限（任一维度），0=通过；等待毫秒客户端本地算
     * （与单维度路径同口径）。weight&lt;=0 时跳过权重 key 操作（两阶段场景入口不预占权重）。</p>
     */
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> TWO_DIM_SCRIPT =
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                    "local over = 0 "
                            + "local p = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                            + "redis.call('PEXPIREAT', KEYS[1], ARGV[3]) "
                            + "if p > tonumber(ARGV[4]) then over = 1 end "
                            + "if tonumber(ARGV[2]) > 0 then "
                            + "  local w = redis.call('INCRBY', KEYS[2], ARGV[2]) "
                            + "  redis.call('PEXPIREAT', KEYS[2], ARGV[3]) "
                            + "  if w > tonumber(ARGV[5]) then over = 1 end "
                            + "end "
                            + "return over",
                    Long.class);

    /**
     * 两阶段权重回填 Lua 脚本：INCRBY 权重 key + 幂等重设窗口终点，不判定（与本地形态 addWeight 同语义）。
     */
    private static final org.springframework.data.redis.core.script.DefaultRedisScript<Long> ADD_WEIGHT_SCRIPT =
            new org.springframework.data.redis.core.script.DefaultRedisScript<>(
                    "local n = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                            + "redis.call('PEXPIREAT', KEYS[1], ARGV[2]) "
                            + "return n",
                    Long.class);

    /**
     * Lua fallback 脚本：INCRBY + PEXPIREAT（窗口终点由客户端传入），返回累加后计数，判定在客户端。
     * <p>
     * KEYS[1]=限流 key，ARGV[1]=permits，ARGV[2]=windowEnd（Unix 毫秒）。返回累加后计数。</p>
     * <p>保持最简形态（不做脚本内判定/不取 PTTL）：限流的绝大多数调用是放行，判定统一在客户端一处，
     * 超限等待毫秒由 {@link #remainMillis} 本地计算（零附加 Redis 命令）——时钟偏差的自愈语义见其 javadoc。</p>
     */
    private static final DefaultRedisScript<Long> FIXED_WINDOW_SCRIPT = new DefaultRedisScript<>(
            "local n = redis.call('INCRBY', KEYS[1], ARGV[1]) "
                    + "redis.call('PEXPIREAT', KEYS[1], ARGV[2]) "
                    + "return n",
            Long.class);

    /**
     * INCREX 能力探测 key（启动时试探一次，短 TTL 自动回收）。
     */
    private static final String INCREX_PROBE_KEY = "uw-kv:increx-probe";

    /**
     * redis 操作模板（String key + Long 值），由 Spring 注入 static 字段。
     */
    private static RedisTemplate<String, Long> longCacheRedisTemplate;

    /**
     * INCREX 能力标记：<b>启动时探测一次</b>（构造器直接试命令，比版本号更准——版本对但不支持命令的
     * fork/代理环境也能识别）。运行期无冷却/重探——Redis 版本在应用运行期不会变化，升级必然伴随
     * 滚动重启（新探测随重启生效）；探测漏网时（启动时 Redis 不可达的乐观默认）运行期遇
     * unknown command 仅一次性翻转为 false，此后固定走 Lua。
     */
    private static volatile boolean increxSupported;

    /**
     * 构造方法，由 Spring 注入 Long RedisTemplate 到 static 字段，并完成 INCREX 能力的启动探测。
     * <p>探测失败（连接异常等非命令错误）时乐观默认支持——运行期 failover 兜底；探测命令为
     * {@code INCREX 探测key BYINT 1 PX 1000}，key 带 1 秒 TTL 自动回收，不污染数据。</p>
     *
     * @param longCacheRedisTemplate Long 值 RedisTemplate
     */
    public GlobalFixedWindowRateLimiter(RedisTemplate<String, Long> longCacheRedisTemplate) {
        GlobalFixedWindowRateLimiter.longCacheRedisTemplate = longCacheRedisTemplate;
        try {
            increx(longCacheRedisTemplate, INCREX_PROBE_KEY, 1, PX_BYTES, "1000");
            increxSupported = true;
        } catch (Exception e) {
            // unknown command=不支持(切Lua);其余异常(连接等)=乐观默认支持,运行期failover兜底
            increxSupported = !isUnknownCommandError(e);
        }
    }

    /**
     * 尝试获取 {@code permits} 个配额（单维度：仅许可数），窗口切换权威由 Redis 侧 PXAT 回收承担。
     *
     * <p>与本地形态 {@code uw.common.util.FixedWindowRateLimiter} 的签名与语义对齐
     * （limitPermits / windowMillis / permits 命名同款、窗口同为墙钟网格对齐、负参防御同口径），
     * 区别仅在作用域（跨实例共享配额）与返回形态（返回 {@code long} 即"0=通过 / &gt;0=remainMillis
     * 等待毫秒"取最轻，与本地形态 Result.remainMillis 同语义——两形态各为其场景最优，勿相互"统一"；
     * 需权重维度时用 {@link #tryAcquire(String, int, long, long, int, long)} 双维度重载）。</p>
     *
     * @param name         限流器名称（决定独立配额池，应含调用方域前缀；空白视为不限流）
     * @param limitPermits 时间窗口内的许可数上限；小于等于 0 表示不限流
     * @param windowMillis 时间窗口长度（毫秒）；小于 1 表示不限流
     * @param permits      本次申请的许可数（负值按 0 处理——负值会变相"返还"配额，可被构造请求绕过限流）
     * @return {@code 0} 表示通过；{@code >0} 表示已超限，需等待的毫秒数（remainMillis，按判定完成时刻
     *         计算的窗口剩余时间，恒大于 0——窗口末尾毫秒发起且往返跨越边界的申请，醒来重试即新窗口）；
     *         Redis 异常时返回 0（降级放行）
     */
    public static long tryAcquire(String name, int limitPermits, long windowMillis, int permits) {
        if (name == null || name.isEmpty() || limitPermits <= 0 || windowMillis < 1) {
            return 0;
        }
        // 防御负入参：负的 permits 会变相"返还"配额，与本地形态 tryAcquire 同口径。
        if (permits < 0) {
            permits = 0;
        }
        long now = SystemClock.now();
        long windowEnd = (now / windowMillis + 1) * windowMillis;
        String key = REDIS_PREFIX + name;
        if (increxSupported) {
            try {
                long count = increx(longCacheRedisTemplate, key, permits, PXAT_BYTES, Long.toString(windowEnd));
                return judgeByCount(count, limitPermits, windowEnd);
            } catch (Exception e) {
                if (isUnknownCommandError(e)) {
                    //启动探测漏网(如启动时Redis不可达的乐观默认):一次性翻转Lua,不重探
                    increxSupported = false;
                    log.warn("Redis不支持INCREX(版本<8.8),限速器已切换Lua路径(重启前不再尝试INCREX): key={}", key);
                    return judgeByCount(executeByLua(key, permits, windowEnd), limitPermits, windowEnd);
                }
                // Redis 异常时降级放行，优先保证调用方可用性
                log.warn("GlobalFixedWindowRateLimiter: 限流检查失败, 降级放行, key=[{}]", key, e);
                return 0;
            }
        }
        return judgeByCount(executeByLua(key, permits, windowEnd), limitPermits, windowEnd);
    }

    /**
     * 尝试获取配额（双维度：许可 + 权重），与本地形态 {@code FixedWindowRateLimiter} 的双维度语义对齐
     * （permits/weight 命名、weight&lt;=0 不预占、任一维度超出即整体拒绝）。
     * <p>双 key 经单段 Lua 原子累加与判定（INCREX 为单 key 命令，双维度场景单命令优势不成立）；
     * key 布局：{@code uw-rate:{name}:p}（许可）与 {@code uw-rate:{name}:w}（权重），与单维度路径的
     * {@code uw-rate:{name}} 互不干扰。两阶段场景：入口传 {@code weight=0}，结束后
     * {@link #addWeight(String, long, long)} 回填实际权重。</p>
     *
     * @param name         限流器名称（应含调用方域前缀；空白视为不限流）
     * @param limitPermits 时间窗口内的许可数上限；小于等于 0 表示许可维度不限
     * @param limitWeight  时间窗口内的权重上限；小于等于 0 表示权重维度不限
     * @param windowMillis 时间窗口长度（毫秒）；小于 1 表示不限流
     * @param permits      本次申请的许可数（负值按 0 处理）
     * @param weight       本次预占的权重；传小于等于 0 表示不预占（两阶段场景事后回填）
     * @return {@code 0} 表示通过；{@code >0} 表示已超限（任一维度），需等待的毫秒数（remainMillis）；
     *         Redis 异常时返回 0（降级放行）
     */
    public static long tryAcquire(String name, int limitPermits, long limitWeight, long windowMillis, int permits, long weight) {
        if (name == null || name.isEmpty() || windowMillis < 1 || (limitPermits <= 0 && limitWeight <= 0)) {
            return 0;
        }
        // 防御负入参：与本地形态同口径。
        if (permits < 0) {
            permits = 0;
        }
        if (weight < 0) {
            weight = 0;
        }
        long now = SystemClock.now();
        long windowEnd = (now / windowMillis + 1) * windowMillis;
        try {
            Long over = longCacheRedisTemplate.execute(TWO_DIM_SCRIPT,
                    Arrays.asList(REDIS_PREFIX + name + ":p", REDIS_PREFIX + name + ":w"),
                    String.valueOf(permits), String.valueOf(weight), String.valueOf(windowEnd),
                    String.valueOf(limitPermits), String.valueOf(limitWeight));
            return over != null && over > 0 ? remainMillis(windowEnd) : 0;
        } catch (Exception e) {
            // Redis 异常时降级放行，优先保证调用方可用性
            log.warn("GlobalFixedWindowRateLimiter: 双维度限流检查失败, 降级放行, key=[{}]", name, e);
            return 0;
        }
    }

    /**
     * 两阶段权重回填：按实际权重累入 {@code uw-rate:{name}:w} 并幂等重设窗口终点，不判定
     * （与本地形态 {@code FixedWindowRateLimiter#addWeight(long)} 同语义——本次不会被拦截，
     * 影响后续调用的权重维度判定；负值按 0 处理，防负回填返还配额）。
     *
     * @param name         限流器名称（须与 tryAcquire 一致）
     * @param windowMillis 时间窗口长度（毫秒），用于计算窗口终点（与 tryAcquire 同参）
     * @param weight       本次回填的权重
     */
    public static void addWeight(String name, long windowMillis, long weight) {
        if (name == null || name.isEmpty() || windowMillis < 1 || weight <= 0) {
            return;
        }
        long windowEnd = (SystemClock.now() / windowMillis + 1) * windowMillis;
        try {
            longCacheRedisTemplate.execute(ADD_WEIGHT_SCRIPT,
                    Collections.singletonList(REDIS_PREFIX + name + ":w"),
                    String.valueOf(weight), String.valueOf(windowEnd));
        } catch (Exception e) {
            log.warn("GlobalFixedWindowRateLimiter: 权重回填失败, key=[{}]", name, e);
        }
    }

    /**
     * 超限等待毫秒：按<b>判定完成时刻</b>取客户端本地时钟计算（主路径与 Lua fallback 共用）。
     * <p>设计取舍：限流的绝大多数调用是放行（1 次往返、零附加），等待毫秒只在少数超限分支发生——
     * 本地计算零附加 Redis 命令、判定逻辑集中一处；不取 Redis PTTL（精度权威但需脚本内嵌判定，
     * 重量与复杂度换来的精度收益仅存在于"超限 × 时钟偏差大"的交集场景）。</p>
     * <p>近似性：窗口翻转的权威时刻是 Redis 侧 PXAT 回收（Redis 时钟），本地偏差大时睡醒重试可能
     * key 尚未回收（再判一次自愈，返回新的剩余）或早已回收（无害）。判定完成时刻重取时钟：
     * Redis 往返可能跨越窗口边界，进入时的 {@code windowEnd - now} 可能为负（负值会使调用方
     * {@code Thread.sleep(负数)} 抛异常）；已越过窗口终点时返回 1（醒来重试即新窗口）。</p>
     *
     * @param windowEnd 窗口终点（Unix 毫秒）
     * @return 恒大于 0 的等待毫秒数
     */
    private static long remainMillis(long windowEnd) {
        long remain = windowEnd - SystemClock.now();
        return remain > 0 ? remain : 1L;
    }

    /**
     * Lua fallback 执行：INCRBY + PEXPIREAT 单往返原子，返回累加后计数（判定在客户端）。
     *
     * @param key        限流 key
     * @param permits    申请数量（上游已 clamp 非负）
     * @param windowEnd  窗口终点（Unix 毫秒）
     * @return 累加后的窗口计数
     */
    private static long executeByLua(String key, int permits, long windowEnd) {
        Long count = longCacheRedisTemplate.execute(FIXED_WINDOW_SCRIPT,
                Collections.singletonList(key), String.valueOf(permits), String.valueOf(windowEnd));
        return count != null ? count : 0;
    }

    /**
     * 按窗口计数判定放行或等待（主路径与 Lua fallback 共用，判定统一在此一处）。
     *
     * @param count        窗口计数
     * @param limitPermits 许可数上限
     * @param windowEnd    窗口终点（Unix 毫秒）
     * @return 0=通过；&gt;0=remainMillis 需等待毫秒
     */
    private static long judgeByCount(long count, int limitPermits, long windowEnd) {
        if (count > limitPermits) {
            return remainMillis(windowEnd);
        }
        return 0;
    }

    /**
     * 发送 INCREX 并返回累加后的新值（命令族公共实现，供 {@link GlobalCounter} 复用）。
     *
     * <p>按过期选项（PX=相对毫秒 / PXAT=绝对毫秒时间戳）发送命令并解析返回。走 Lettuce 原生 dispatch +
     * {@link LongArrayOutput}：Spring Data Redis 的 {@code connection.execute(命令名, 参数)} 对未收录的新命令
     * 用 ByteArrayOutput 解析，而 INCREX 返回 integer 数组，ByteArrayOutput 不支持 {@code set(long)}，
     * 客户端解码直接抛 UnsupportedOperationException（服务端命令实际已执行成功）。</p>
     *
     * <p>执行异常（Redis 故障等）原样上抛，由调用方决定重试或降级。返回形态意外时（增量已入 Redis）补一次
     * 标准 GET 取全局现值（GET 返回 ≥ 本次增量，方向安全），而不是报错——调用方会把执行失败当作未生效处理，
     * 下轮重推将多记。</p>
     *
     * @param template       Long 值 RedisTemplate
     * @param key            完整 redis key
     * @param incrementNum   增量
     * @param expireOption   过期选项字面量（{@link #PX_BYTES} / {@link #PXAT_BYTES}）
     * @param expireValue    过期值（相对毫秒或 Unix 绝对毫秒）
     * @return 累加后的新值
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static long increx(final RedisTemplate<String, Long> template, final String key, final long incrementNum,
                       final byte[] expireOption, final String expireValue) {
        RedisSerializer keySerializer = template.getKeySerializer();
        byte[] keyBytes = keySerializer != null ? keySerializer.serialize(key) : key.getBytes(StandardCharsets.UTF_8);
        List<Long> result = template.execute((RedisCallback<List<Long>>) connection -> {
            CommandArgs<byte[], byte[]> args = new CommandArgs<>(ByteArrayCodec.INSTANCE)
                    .addKey(keyBytes)
                    .add(BYINT_BYTES).add(Long.toString(incrementNum).getBytes(StandardCharsets.UTF_8))
                    .add(expireOption).add(expireValue.getBytes(StandardCharsets.UTF_8));
            Object nativeConnection = connection.getNativeConnection();
            try {
                //常规路径:共享异步命令视图(Spring LettuceConnection 的 native 为 RedisAsyncCommandsImpl)
                if (nativeConnection instanceof BaseRedisAsyncCommands<?, ?> asyncCommands) {
                    RedisFuture<List<Long>> future = ((BaseRedisAsyncCommands<byte[], byte[]>) asyncCommands)
                            .dispatch(INCREX_KEYWORD, new LongArrayOutput(), args);
                    return future.get(10, TimeUnit.SECONDS);
                }
                //防御路径:同步视图直接 dispatch
                if (nativeConnection instanceof RedisCommands) {
                    RedisCommands<byte[], byte[]> commands = (RedisCommands<byte[], byte[]>) nativeConnection;
                    return commands.dispatch(INCREX_KEYWORD, new LongArrayOutput(), args);
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                throw new IllegalStateException("INCREX执行失败, key=" + key, e.getCause() != null ? e.getCause() : e);
            }
            throw new IllegalStateException("INCREX需要Lettuce原生连接,当前: " + nativeConnection.getClass().getName());
        });
        //INCREX 整数模式返回 [新值, 实际增量],取第一个元素
        if (result != null && !result.isEmpty() && result.get(0) != null) {
            return result.get(0);
        }
        log.error("INCREX返回形态解析失败,退回标准GET取全局现值, key={}, result={}", key, result);
        Long current = template.opsForValue().get(key);
        return current != null ? current : 0L;
    }

    /**
     * 判断异常链中是否为 "unknown command" 类服务端错误（服务端无 INCREX 命令的特征，即版本 &lt; 8.8）。
     * <p>遍历 cause 链匹配 {@link io.lettuce.core.RedisCommandExecutionException} 的 message，
     * 与连接类故障（RedisConnectionFailureException 等）区分——后者不触发能力降级，应照常上抛。</p>
     *
     * @param e 待判定异常
     * @return true 表示服务端不支持 INCREX
     */
    static boolean isUnknownCommandError(Throwable e) {
        Throwable t = e;
        for (int i = 0; t != null && i < 10; i++) {
            if (t instanceof io.lettuce.core.RedisCommandExecutionException
                    && t.getMessage() != null && t.getMessage().toLowerCase().contains("unknown command")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /**
     * 测试辅助：强制走 Lua 路径（验证降级；生产代码勿用）。
     */
    static void markIncrexUnavailableForTest() {
        increxSupported = false;
    }

    /**
     * 测试辅助：恢复 INCREX 路径（生产代码勿用）。
     */
    static void resetIncrexUnavailableForTest() {
        increxSupported = true;
    }

    /**
     * INCREX 返回的 integer 数组解析器（[新值, 实际应用增量]）。
     * <p>
     * Lettuce 内置输出无 integer 数组形态（ByteArrayOutput 不支持 set(long)），自定义接收；
     * 同时覆写 set(ByteBuffer) 兼容 bulk 字符串形态的数字元素（防御协议实现差异）。
     */
    private static final class LongArrayOutput extends CommandOutput<byte[], byte[], List<Long>> {

        LongArrayOutput() {
            super(ByteArrayCodec.INSTANCE, new ArrayList<>(2));
        }

        @Override
        public void set(long integer) {
            output.add(integer);
        }

        @Override
        public void set(ByteBuffer bytes) {
            output.add(bytes == null ? 0L : Long.parseLong(decodeString(bytes)));
        }
    }

}
