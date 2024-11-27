package uw.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.StringJoiner;

/**
 * 用于作为返回给前端的vo类。
 * 支持链式调用，同时支持泛型。
 *
 * @author axeon
 */
@Schema(title = "返回信息结构体", description = "返回信息结构体")
public final class ResponseData<T> {

    /**
     * 成功状态值。
     */
    public static final String STATE_SUCCESS = "success";
    /**
     * 报警状态值。
     */
    public static final String STATE_WARN = "warn";
    /**
     * 错误状态值。
     */
    public static final String STATE_ERROR = "error";
    /**
     * 位置状态值。
     */
    public static final String STATE_UNKNOWN = "unknown";

    /**
     * 成功常量，不带时间戳。
     */
    public static final ResponseData SUCCESS = new ResponseData( STATE_SUCCESS );

    /**
     * 报警常量，不带时间戳。
     */
    public static final ResponseData WARN = new ResponseData( STATE_WARN );

    /**
     * 失败常量，不带时间戳。
     */
    public static final ResponseData ERROR = new ResponseData( STATE_ERROR );


    /**
     * 响应时间
     */
    @Schema(title = "时间戳", description = "时间戳")
    private long timestamp;

    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态")
    private String state = STATE_UNKNOWN;

    /**
     * 信息。
     */
    @Schema(title = "信息", description = "信息")
    private String msg;

    /**
     * 自定义代码，一般情况下代表错误代码。
     * 请尽量不要使用此代码存储http code，没用。
     * 此代码将来可用于i18n的错误信息输出。
     */
    @Schema(title = "自定义代码", description = "自定义代码")
    private String code;

    /**
     * 返回数据
     */
    @Schema(title = "返回数据")
    private T data;

    private ResponseData(T data, String state, String code, String msg) {
        this.timestamp = System.currentTimeMillis();
        this.state = state;
        this.msg = msg;
        this.code = code;
        this.data = data;
    }

    private ResponseData(String state) {
        this.state = state;
    }

    /**
     * 默认构造器，主要为json序列化使用。
     */
    public ResponseData() {
        super();
    }

    /**
     * 无数值成功。
     *
     * @return
     */
    public static <T> ResponseData<T> success() {
        return new ResponseData<T>( null, STATE_SUCCESS, null, null );
    }

    /**
     * 带结果成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t) {
        return new ResponseData<T>( t, STATE_SUCCESS, null, null );
    }

    /**
     * 带结果和代码的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, String code) {
        return new ResponseData<T>( t, STATE_SUCCESS, code, null );
    }

    /**
     * 带结果、代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, String code, String msg) {
        return new ResponseData<T>( t, STATE_SUCCESS, code, msg );
    }

    /**
     * 带代码的成功返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> successCode(String code) {
        return new ResponseData<T>( null, STATE_SUCCESS, code, null );
    }

    /**
     * 带代码，消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> successCode(String code, String msg) {
        return new ResponseData<T>( null, STATE_SUCCESS, code, msg );
    }


    /**
     * 消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> successMsg(String msg) {
        return new ResponseData<T>( null, STATE_SUCCESS, null, msg );
    }


    /**
     * 无数值警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn() {
        return new ResponseData<T>( null, STATE_WARN, null, null );
    }

    /**
     * 带结果的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t) {
        return new ResponseData<T>( t, STATE_WARN, null, null );
    }

    /**
     * 带结果和代码的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t, String code) {
        return new ResponseData<T>( t, STATE_WARN, code, null );
    }

    /**
     * 带结果、代码和消息的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t, String code, String msg) {
        return new ResponseData<T>( t, STATE_WARN, code, msg );
    }

    /**
     * 带代码的警告返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> warnCode(String code) {
        return new ResponseData<T>( null, STATE_WARN, code, null );
    }

    /**
     * 带代码，消息的警告返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> warnCode(String code, String msg) {
        return new ResponseData<T>( null, STATE_WARN, code, msg );
    }

    /**
     * 消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> warnMsg(String msg) {
        return new ResponseData<T>( null, STATE_WARN, null, msg );
    }


    /**
     * 无数值的失败返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> error() {
        return new ResponseData<T>( null, STATE_ERROR, null, null );
    }

    /**
     * 附带代码的失败返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> error(T t, String code) {
        return new ResponseData<T>( t, STATE_ERROR, code, null );
    }

    /**
     * 带结果、代码和消息的失败返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> error(T t, String code, String msg) {
        return new ResponseData<T>( t, STATE_ERROR, code, msg );
    }

    /**
     * 带代码的失败返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> errorCode(String code) {
        return new ResponseData<T>( null, STATE_ERROR, code, null );
    }

    /**
     * 带代码，消息的失败返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> errorCode(String code, String msg) {
        return new ResponseData<T>( null, STATE_ERROR, code, msg );
    }

    /**
     * 消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> errorMsg(String msg) {
        return new ResponseData<T>( null, STATE_ERROR, null, msg );
    }


    /**
     * 是否成功。
     *
     * @return
     */
    @JsonIgnore
    public boolean isSuccess() {
        return STATE_SUCCESS.equals( this.state );
    }

    /**
     * 是否不成功。
     *
     * @return
     */
    @JsonIgnore
    public boolean isNotSuccess() {
        return !STATE_SUCCESS.equals( this.state );
    }

    /**
     * 是否有警报
     *
     * @return
     */
    @JsonIgnore
    public boolean isWarn() {
        return STATE_WARN.equals( this.state );
    }

    /**
     * 是否有错误
     *
     * @return
     */
    @JsonIgnore
    public boolean isError() {
        return STATE_ERROR.equals( this.state );
    }

    /**
     * 是否没有错误
     *
     * @return
     */
    @JsonIgnore
    public boolean isNotError() {
        return !STATE_ERROR.equals( this.state );
    }

    /**
     * 获得原型。
     * 主要用于某些情况下抹除泛型标记。
     *
     * @return
     */
    public ResponseData prototype() {
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner( ", ", ResponseData.class.getSimpleName() + "[", "]" ).add( "timestamp=" + timestamp ).add( "state='" + state + "'" ).add( "msg='" + msg + "'" ).add( "code='" + code + "'" ).add( "data=" + data ).toString();
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
