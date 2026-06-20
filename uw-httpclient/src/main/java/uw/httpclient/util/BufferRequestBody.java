package uw.httpclient.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;

/**
 * 基于内存缓冲区（{@link Buffer}）构造的 OkHttp {@link RequestBody} 工具。
 * <p>
 * 用于将一个已经存在于内存的 okio {@link Buffer} 以零拷贝方式写入 Socket，
 * 避免不必要的字节复制。适用于流式上传场景。
 *
 * @since 2018-04-27
 */
public final class BufferRequestBody {

    private BufferRequestBody() {
    }

    /**
     * 创建一个以指定 {@code buffer} 为数据源的 RequestBody。
     *
     * @param buffer      数据源缓冲区，不能为 null。
     * @param contentType 请求体 MIME 类型，可为 null。
     * @return 写入时直接透传 buffer 内容的 RequestBody。
     * @throws NullPointerException 当 buffer 为 null 时抛出。
     */
    public static RequestBody create(final Buffer buffer, final MediaType contentType) {
        if (buffer == null) throw new NullPointerException("buffer == null");
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return -1;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                // zero-copy impl,NB
                sink.write(buffer, buffer.size());
            }
        };
    }
}
