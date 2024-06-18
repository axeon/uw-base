package uw.httpclient.util;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;

import java.io.IOException;

/**
 * 以一个流作为源写入Socket。
 *
 * 
 * @since 2018-04-27
 */
public final class BufferRequestBody {

    /**
     * Returns a new request body that transmits the content of {@code buffer}.
     *
     * @param buffer
     * @param contentType
     * @return
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
