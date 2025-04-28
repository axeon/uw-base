package uw.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import uw.common.util.JsonUtils;
import uw.common.util.SystemClock;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * ResponseData是一个基于泛型的响应数据对象，封装了响应数据，以及响应状态码。
 * 在整个项目中，所有响应数据都使用ResponseData进行封装，包括返回给前端的数据，以及返回给后端的数据。
 * 使用ResponseData的好处是，可以统一处理响应数据，尤其在返回数据的同时，也返回了响应状态码和消息，使得程序可以更好的处理响应数据。这样也避免了不必要的异常处理和异常捕获代码，可能获取更简单和更优雅的代码。
 * 同时，ResponseData和i18n的结合，使得处理国际化的工作更加简单和方便。
 * ResponseCode定义了响应状态码和消息的统一接口，通过继承关系，可以定义各种状态码和消息，使得代码更加清晰和可读。
 * ResponseData通过传入ResponseCode，简化调用代码，同时更简单的支持多语言。
 *
 * @author axeon
 */
@Schema(title = "返回信息结构体", description = "返回信息结构体")
public class ResponseData<T> {

    /**
     * 成功状态值。
     */
    public static final String STATE_SUCCESS = "success";
    /**
     * 警告状态值。警告信息可以忽略，返回的数据仍可使用。
     */
    public static final String STATE_WARN = "warn";
    /**
     * 错误状态值。错误信息表示执行失败，返回的数据应不可用。
     */
    public static final String STATE_ERROR = "error";
    /**
     * 严重错误状态值。系统已经报错，返回数据不可用，需要立即处理。
     */
    public static final String STATE_FATAL = "fatal";
    /**
     * 未知状态值
     */
    public static final String STATE_UNKNOWN = "unknown";
    /**
     * 成功常量，不带时间戳。
     */
    public static final ResponseData SUCCESS = new ResponseData(0, null, STATE_SUCCESS, null, null);
    /**
     * 警告常量，不带时间戳。
     */
    public static final ResponseData WARN = new ResponseData(0, STATE_WARN, null, null);
    /**
     * 失败常量，不带时间戳。
     */
    public static final ResponseData ERROR = new ResponseData(0, STATE_ERROR, null, null);
    /**
     * 严重错误常量，不带时间戳。
     */
    public static final ResponseData FATAL = new ResponseData(0, STATE_FATAL, null, null);
    /**
     * 响应时间
     */
    @Schema(title = "时间戳", description = "时间戳")
    private long time;
    /**
     * 状态
     */
    @Schema(title = "状态", description = "状态")
    private String state = STATE_UNKNOWN;
    /**
     * 信息。
     */
    @Schema(title = "消息", description = "消息")
    private String msg;
    /**
     * 自定义代码，一般情况下代表错误代码。
     * 请尽量不要使用此代码存储http code，没用。
     * 此代码将来可用于i18n的错误信息输出。
     */
    @Schema(title = "代码", description = "代码")
    private String code;
    /**
     * 返回数据
     */
    @Schema(title = "数据")
    private T data;

    /**
     * 构造器。
     *
     * @param data
     * @param state
     * @param code
     * @param msg
     */
    private ResponseData(long time, T data, String state, String code, String msg) {
        this.time = time;
        this.state = state;
        this.msg = msg;
        this.code = code;
        this.data = data;
    }

    /**
     * 构造器。
     * 默认带当前时间。
     *
     * @param data
     * @param state
     * @param code
     * @param msg
     */
    private ResponseData(T data, String state, String code, String msg) {
        this.time = SystemClock.now();
        this.state = state;
        this.msg = msg;
        this.code = code;
        this.data = data;
    }

    /**
     * 默认构造器，主要为json序列化使用。
     */
    public ResponseData() {
    }

    /**
     * 静态构造器。
     *
     * @param data
     * @param state
     * @param code
     * @param msg
     */
    public static <T> ResponseData<T> of(long time, T data, String state, String code, String msg) {
        return new ResponseData<T>(time, data, state, code, msg);
    }

    /**
     * 静态构造器。
     *
     * @param data
     * @param state
     * @param code
     * @param msg
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> of(T data, String state, String code, String msg) {
        return new ResponseData<T>(SystemClock.now(), data, state, code, msg);
    }

    /**
     * 静态构造器。
     *
     * @param state
     * @param code
     * @param msg
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> of(String state, String code, String msg) {
        return new ResponseData<T>(SystemClock.now(), null, state, code, msg);
    }

    /**
     * 无数值成功。
     *
     * @return
     */
    public static <T> ResponseData<T> success() {
        return new ResponseData<T>(null, STATE_SUCCESS, null, null);
    }

    /**
     * 带结果成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t) {
        return new ResponseData<T>(t, STATE_SUCCESS, null, null);
    }

    /**
     * 带结果和代码的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, String code) {
        return new ResponseData<T>(t, STATE_SUCCESS, code, null);
    }

    /**
     * 带结果、代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, String code, String msg) {
        return new ResponseData<T>(t, STATE_SUCCESS, code, msg);
    }

    /**
     * 带结果、代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, ResponseData responseData) {
        return new ResponseData<T>(t, STATE_SUCCESS, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带结果、代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> success(T t, ResponseCode responseCode) {
        return new ResponseData<T>(t, STATE_SUCCESS, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带结果、代码和消息的成功返回值，支持参数化消息格式。
     *
     * @param t            返回数据
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> success(T t, ResponseCode responseCode, Object... params) {
        return new ResponseData<>(t, STATE_SUCCESS, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 带代码的成功返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> successCode(String code) {
        return new ResponseData<T>(null, STATE_SUCCESS, code, null);
    }

    /**
     * 带代码，消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> successCode(String code, String msg) {
        return new ResponseData<T>(null, STATE_SUCCESS, code, msg);
    }

    /**
     * 带代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> successCode(ResponseCode responseCode) {
        return new ResponseData<T>(null, STATE_SUCCESS, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带代码和消息的成功返回值，支持参数化消息格式。
     *
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> successCode(ResponseCode responseCode, Object... params) {
        return new ResponseData<>(null, STATE_SUCCESS, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 带代码和消息的成功返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> successCode(ResponseData responseData) {
        return new ResponseData<T>(null, STATE_SUCCESS, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 消息的成功返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> successMsg(String msg) {
        return new ResponseData<T>(null, STATE_SUCCESS, null, msg);
    }

    /**
     * 无数值警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn() {
        return new ResponseData<T>(null, STATE_WARN, null, null);
    }

    /**
     * 带结果的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t) {
        return new ResponseData<T>(t, STATE_WARN, null, null);
    }

    /**
     * 带结果和代码的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t, String code) {
        return new ResponseData<T>(t, STATE_WARN, code, null);
    }

    /**
     * 带结果、代码和消息的警告返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> warn(T t, String code, String msg) {
        return new ResponseData<T>(t, STATE_WARN, code, msg);
    }

    /**
     * 带结果、代码和消息的警告返回值。
     *
     * @param t
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> warn(T t, ResponseData responseData) {
        return new ResponseData<T>(t, STATE_WARN, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带结果、代码和消息的警告返回值。
     *
     * @param t
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> warn(T t, ResponseCode responseCode) {
        return new ResponseData<T>(t, STATE_WARN, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带结果、代码和消息的警告返回值，支持参数化消息格式。
     *
     * @param t            返回数据
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> warn(T t, ResponseCode responseCode, Object... params) {
        return new ResponseData<>(t, STATE_WARN, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 带代码的警告返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> warnCode(String code) {
        return new ResponseData<T>(null, STATE_WARN, code, null);
    }

    /**
     * 带代码，消息的警告返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> warnCode(String code, String msg) {
        return new ResponseData<T>(null, STATE_WARN, code, msg);
    }

    /**
     * 带结果、代码和消息的警告返回值。
     *
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> warnCode(ResponseData responseData) {
        return new ResponseData<T>(null, STATE_WARN, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带代码和消息的警告返回值。
     *
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> warnCode(ResponseCode responseCode) {
        return new ResponseData<T>(null, STATE_WARN, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带代码和消息的警告返回值，支持参数化消息格式。
     *
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> warnCode(ResponseCode responseCode, Object... params) {
        return new ResponseData<>(null, STATE_WARN, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 消息的警告返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> warnMsg(String msg) {
        return new ResponseData<T>(null, STATE_WARN, null, msg);
    }

    /**
     * 无数值的失败返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> error() {
        return new ResponseData<T>(null, STATE_ERROR, null, null);
    }

    /**
     * 失败返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> error(T t) {
        return new ResponseData<T>(t, STATE_ERROR,null, null);
    }

    /**
     * 附带代码的失败返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> error(T t, String code) {
        return new ResponseData<T>(t, STATE_ERROR, code, null);
    }

    /**
     * 带结果、代码和消息的失败返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> error(T t, String code, String msg) {
        return new ResponseData<T>(t, STATE_ERROR, code, msg);
    }

    /**
     * 带结果、代码和消息的失败返回值。
     *
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> error(T t, ResponseData responseData) {
        return new ResponseData<T>(t, STATE_ERROR, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带结果、代码和消息的失败返回值。
     *
     * @param t
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> error(T t, ResponseCode responseCode) {
        return new ResponseData<T>(t, STATE_ERROR, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带结果、代码和消息的失败返回值，支持参数化消息格式。
     *
     * @param t            返回数据
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> error(T t, ResponseCode responseCode, Object... params) {
        return new ResponseData<>(t, STATE_ERROR, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 带代码的失败返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> errorCode(String code) {
        return new ResponseData<T>(null, STATE_ERROR, code, null);
    }

    /**
     * 带代码，消息的失败返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> errorCode(String code, String msg) {
        return new ResponseData<T>(null, STATE_ERROR, code, msg);
    }

    /**
     * 带代码，消息的失败返回值。
     *
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> errorCode(ResponseData responseData) {
        return new ResponseData<T>(null, STATE_ERROR, responseData.getCode(), responseData.getMsg());
    }


    /**
     * 带代码和消息的失败返回值。
     *
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> errorCode(ResponseCode responseCode) {
        return new ResponseData<T>(null, STATE_ERROR, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带代码和消息的失败返回值，支持参数化消息格式。
     *
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> errorCode(ResponseCode responseCode, Object... params) {
        return new ResponseData<>(null, STATE_ERROR, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 消息的失败返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> errorMsg(String msg) {
        return new ResponseData<T>(null, STATE_ERROR, null, msg);
    }

    /**
     * 无数值的严重错误返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> fatal() {
        return new ResponseData<T>(null, STATE_FATAL, null, null);
    }

    /**
     * 严重错误返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> fatal(T t) {
        return new ResponseData<T>(t, STATE_FATAL,null, null);
    }


    /**
     * 附带代码的严重错误返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> fatal(T t, String code) {
        return new ResponseData<T>(t, STATE_FATAL, code, null);
    }

    /**
     * 带结果、代码和消息的严重错误返回值。
     *
     * @return
     */
    public static <T> ResponseData<T> fatal(T t, String code, String msg) {
        return new ResponseData<T>(t, STATE_FATAL, code, msg);
    }

    /**
     * 带结果、代码和消息的严重失败返回值。
     *
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> fatal(T t, ResponseData responseData) {
        return new ResponseData<T>(t, STATE_ERROR, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带结果、代码和消息的严重错误返回值。
     *
     * @param t
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> fatal(T t, ResponseCode responseCode) {
        return new ResponseData<T>(t, STATE_FATAL, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带结果、代码和消息的严重错误返回值，支持参数化消息格式。
     *
     * @param t            返回数据
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> fatal(T t, ResponseCode responseCode, Object... params) {
        return new ResponseData<>(t, STATE_FATAL, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 带代码的严重错误返回值。
     *
     * @param code
     * @return
     */
    public static <T> ResponseData<T> fatalCode(String code) {
        return new ResponseData<T>(null, STATE_FATAL, code, null);
    }

    /**
     * 带代码，消息的严重错误返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> fatalCode(String code, String msg) {
        return new ResponseData<T>(null, STATE_FATAL, code, msg);
    }

    /**
     * 带代码，消息的严重错误返回值。
     *
     * @param responseData
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> fatalCode(ResponseData responseData) {
        return new ResponseData<T>(null, STATE_FATAL, responseData.getCode(), responseData.getMsg());
    }

    /**
     * 带代码和消息的严重错误返回值。
     *
     * @param responseCode
     * @param <T>
     * @return
     */
    public static <T> ResponseData<T> fatalCode(ResponseCode responseCode) {
        return new ResponseData<T>(null, STATE_FATAL, responseCode.getFullCode(), responseCode.getLocalizedMessage());
    }

    /**
     * 带代码和消息的严重错误返回值，支持参数化消息格式。
     *
     * @param responseCode 响应代码对象
     * @param params       用于格式化消息的参数（如i18n占位符替换）
     * @param <T>          泛型类型
     * @return ResponseData实例
     */
    public static <T> ResponseData<T> fatalCode(ResponseCode responseCode, Object... params) {
        return new ResponseData<>(null, STATE_FATAL, responseCode.getFullCode(), responseCode.getLocalizedMessage(params));
    }

    /**
     * 消息的严重错误返回值。
     *
     * @param msg
     * @return
     */
    public static <T> ResponseData<T> fatalMsg(String msg) {
        return new ResponseData<T>(null, STATE_FATAL, null, msg);
    }

    /**
     * 是否成功。
     *
     * @return
     */
    @JsonIgnore
    public boolean isSuccess() {
        return STATE_SUCCESS.equals(this.state);
    }

    /**
     * 成功时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onSuccess(Function<T, ResponseData<R>> function) {
        if (isSuccess()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 成功时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onSuccess(Consumer<T> consumer) {
        if (isSuccess()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 成功时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onSuccess(Runnable runnable) {
        if (isSuccess()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * 是否不成功。
     *
     * @return
     */
    @JsonIgnore
    public boolean isNotSuccess() {
        return !STATE_SUCCESS.equals(this.state);
    }


    /**
     * 不成功时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onNotSuccess(Function<T, ResponseData<R>> function) {
        if (isNotSuccess()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 不成功时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onNotSuccess(Consumer<T> consumer) {
        if (isNotSuccess()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 不成功时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onNotSuccess(Runnable runnable) {
        if (isNotSuccess()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * 是否有警报
     *
     * @return
     */
    @JsonIgnore
    public boolean isWarn() {
        return STATE_WARN.equals(this.state);
    }

    /**
     * 警告时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onWarn(Function<T, ResponseData<R>> function) {
        if (isWarn()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 警告时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onWarn(Consumer<T> consumer) {
        if (isWarn()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 警告时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onWarn(Runnable runnable) {
        if (isWarn()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * 是否有错误
     *
     * @return
     */
    @JsonIgnore
    public boolean isError() {
        return STATE_ERROR.equals(this.state) || STATE_FATAL.equals(this.state);
    }

    /**
     * 错误时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onError(Function<T, ResponseData<R>> function) {
        if (isError()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 错误时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onError(Consumer<T> consumer) {
        if (isError()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 错误时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onError(Runnable runnable) {
        if (isError()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * 是否严重错误
     *
     * @return
     */
    @JsonIgnore
    public boolean isFatal() {
        return STATE_FATAL.equals(this.state);
    }

    /**
     * 严重错误时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onFatal(Function<T, ResponseData<R>> function) {
        if (isFatal()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 严重错误时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onFatal(Consumer<T> consumer) {
        if (isFatal()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 严重错误时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onFatal(Runnable runnable) {
        if (isFatal()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * 判断是否既非错误状态也非严重错误状态
     *
     * @return
     */
    @JsonIgnore
    public boolean isNotError() {
        return !STATE_ERROR.equals(this.state) && !STATE_FATAL.equals(this.state);
    }


    /**
     * 不是错误时执行。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> ResponseData<R> onNotError(Function<T, ResponseData<R>> function) {
        if (isNotError()) {
            return function.apply(data);
        }
        return this.raw();
    }

    /**
     * 不是错误时执行。
     *
     * @param consumer
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onNotError(Consumer<T> consumer) {
        if (isNotError()) {
            consumer.accept(data);
        }
        return this.raw();
    }

    /**
     * 不是错误时执行。
     *
     * @param runnable
     * @return
     */
    @JsonIgnore
    public ResponseData<T> onNotError(Runnable runnable) {
        if (isNotError()) {
            runnable.run();
        }
        return this.raw();
    }

    /**
     * map类型转换。
     *
     * @param function
     * @param <R>
     * @return
     */
    @JsonIgnore
    public <R> R map(Function<ResponseData<T>, R> function) {
            return function.apply(this);
    }

    /**
     * 返回未泛型化的原始响应对象实例。
     * 主要用于需要抹除泛型类型参数的场景（如某些框架要求非泛型对象）。
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public ResponseData raw() {
        return this;
    }

    /**
     * 转换为字符串。
     *
     * @return
     */
    @Override
    public String toString() {
        return JsonUtils.toString(this);
    }


    /**
     * 获取状态。
     *
     * @return
     */
    public String getState() {
        return state;
    }

    /**
     * 设置状态。
     *
     * @param state
     */
    public void setState(String state) {
        this.state = state;
    }


    /**
     * 获取数据。
     *
     * @return
     */
    public T getData() {
        return data;
    }

    /**
     * 设置数据。
     *
     * @param data
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 设置数据。
     *
     * @param data
     * @return
     */
    @JsonIgnore
    public ResponseData<T> data(T data) {
        this.data = data;
        return this;
    }

    /**
     * 获取代码。
     *
     * @return
     */
    public String getCode() {
        return code;
    }

    /**
     * 设置代码。
     *
     * @param code
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * 设置代码。
     *
     * @param code
     * @return
     */
    @JsonIgnore
    public ResponseData<T> code(String code) {
        this.code = code;
        return this;
    }

    /**
     * 获取消息。
     *
     * @return
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置消息。
     *
     * @param msg
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * @param msg
     * @return
     */
    @JsonIgnore
    public ResponseData<T> msg(String msg) {
        this.msg = msg;
        return this;
    }

    /**
     * 获取时间戳。
     *
     * @return
     */
    public long getTime() {
        return time;
    }

    /**
     * 设置时间戳。
     *
     * @param time
     */
    public void setTime(long time) {
        this.time = time;
    }

    /**
     * 获取数据类型。
     *
     * @return
     */
    public String getType() {
        return data == null ? null : data.getClass().getSimpleName();
    }
}
