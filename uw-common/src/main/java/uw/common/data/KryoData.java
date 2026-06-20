package uw.common.data;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.Serializable;

/**
 * Kryo 手工序列化数据接口。
 * <p>
 * 实现本接口的数据类，由自身负责通过 kryo 的 {@link Output}/{@link Input} 原语方法
 * （writeVarLong/readVarLong/writeString/readString 等）完成字段的逐个读写，
 * 配合 {@link uw.common.util.KryoUtils#serializeData(KryoData)} /
 * {@link uw.common.util.KryoUtils#deserializeData(byte[], KryoData)} 完成序列化/反序列化。
 *
 * <h2>三种序列化方式的定位</h2>
 * <ul>
 *   <li><b>整对象读写</b>（{@code KryoUtils.serialize(Object)}）：kryo 反射自动读写，最省心但有反射开销，且未注册类走全限定类名路径体积偏大。</li>
 *   <li><b>lambda 手工读写</b>（{@code KryoUtils.serialize(Consumer)}）：调用方用 lambda 写原语，灵活但读写逻辑散落在调用处。</li>
 *   <li><b>本接口式</b>（实现 KryoData）：读写逻辑<b>内聚到数据类自身</b>，一处定义、多处复用，适合作为"数据契约"的数据类。</li>
 * </ul>
 *
 * <h2>实现要点</h2>
 * <ul>
 *   <li><b>读写顺序必须一致</b>：{@code serialize} 写入的字段顺序与类型，必须与 {@code deserialize} 读取的完全对应，
 *       否则反序列化错位、数据损坏。建议在两个方法里按字段声明顺序逐一对应。</li>
 *   <li><b>需有无参构造</b>：{@code KryoUtils.deserializeData} 由调用方创建实例后传入，数据类需提供公开无参构造（或由调用方确保实例已创建）。</li>
 *   <li><b>null 字段约定</b>：原语读写不直接表达 null，字符串/集合为 null 时应约定固定写法（如写空串、写长度 0），由实现类自行保证读写一致。</li>
 *   <li><b>不支持循环引用</b>：本接口走纯原语路径，KryoUtils 配置 {@code references=false}，对象图不能有环。</li>
 * </ul>
 *
 * <h2>与 kryo 内置 KryoSerializable 的区别</h2>
 * <ul>
 *   <li><b>KryoSerializable</b>：{@code write(Kryo, Output)} / {@code read(Kryo, Input)}，带 Kryo 参数，
 *       经 {@code kryo.writeObject} 调用，<b>走池</b>（serializePool），有池竞争但 Output 复用无 GC 垃圾。</li>
 *   <li><b>本接口 KryoData</b>：{@code serialize(Output)} / {@code deserialize(Input)}，不带 Kryo，
 *       走 {@code KryoUtils.serializeData/deserializeData} 的<b>无池路径</b>（每次 new Output/Input），
 *       多线程吞吐更高（零池竞争）但有 GC 成本（见 {@link uw.common.util.KryoUtils#serializeData(int, KryoData)}）。</li>
 *   <li>选择：不需要 kryo.writeObject 的纯原语场景用本接口（更轻量、吞吐高）；需要在手工协议里混用 kryo 对象级序列化时用 KryoSerializable。</li>
 * </ul>
 *
 * <h2>示例</h2>
 * <pre>{@code
 * public class UserInfo implements KryoData {
 *     private long userId;
 *     private String name;
 *
 *     public UserInfo() {} // 无参构造，供反序列化创建实例
 *
 *     @Override
 *     public void serialize(Output output) {
 *         output.writeVarLong(userId, true);
 *         output.writeString(name == null ? "" : name); // null 约定写空串
 *     }
 *
 *     @Override
 *     public void deserialize(Input input) {
 *         userId = input.readVarLong(true);
 *         name = input.readString();
 *     }
 * }
 *
 * // 序列化
 * byte[] bytes = KryoUtils.serializeData(userInfo);
 * // 反序列化
 * UserInfo restored = KryoUtils.deserializeData(bytes, new UserInfo());
 * }</pre>
 *
 * @author axeon
 */
public interface KryoData extends Serializable {

    /**
     * 将本对象的字段写入 Output（打包/序列化）。
     * <p>
     * 用 kryo 原语方法（writeVarLong/writeString/writeInt 等）按约定顺序写入。
     * 写入顺序必须与 {@link #deserialize(Input)} 的读取顺序完全一致。
     * <p>
     * 异常策略：kryo 原语方法抛 {@code KryoException}（RuntimeException），本接口不声明 checked exception，
     * 实现类无需声明 throws，异常直接上浮由调用方处理。与 KryoUtils 其他序列化方法的异常策略保持一致。
     *
     * @param output kryo 输出流，由 KryoUtils 借出
     */
    void serialize(Output output);

    /**
     * 从 Input 读取字段填充本对象（解包/反序列化）。
     * <p>
     * 用 kryo 原语方法（readVarLong/readString/readInt 等）按 {@link #serialize(Output)} 的相同顺序读取。
     * 调用前对象已由 KryoUtils（或调用方）创建，本方法只负责填字段。
     * <p>
     * 异常策略：同 {@link #serialize(Output)}，抛 RuntimeException，不声明 checked exception。
     *
     * @param input kryo 输入流，由 KryoUtils 借出
     */
    void deserialize(Input input);
}