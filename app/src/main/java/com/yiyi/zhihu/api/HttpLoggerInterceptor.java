package com.yiyi.zhihu.api;


import com.yiyi.zhihu.util.JsonUtils;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.concurrent.TimeUnit;

import okhttp3.Connection;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static okhttp3.internal.http.StatusLine.HTTP_CONTINUE;

/**
 * ©2016-2017 kmhealthcloud.All Rights Reserved <p/>
 * Created by: L  <br/>
 * Description:
 */
public class HttpLoggerInterceptor implements Interceptor
{

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final String sTag = "L:okhttp";
    private final String mTag;
    private final Logger logger;
    private volatile Level level = Level.BODY;

    public HttpLoggerInterceptor()
    {
        this(Logger.DEFAULT, null);
    }

    public HttpLoggerInterceptor(Logger logger)
    {
        this(logger, null);
    }

    public HttpLoggerInterceptor(Logger logger, String tag)
    {
        this.logger = logger;
        mTag = TextUtils.isEmpty(tag) ? "L:okhttp" : tag;
    }

    public static long contentLength(Response response)
    {
        return contentLength(response.headers());
    }

    public static long contentLength(Headers headers)
    {
        return stringToLong(headers.get("Content-Length"));
    }

    /**
     * Returns true if the response must have a (possibly 0-length) body. See RFC 2616 section 4.3.
     */
    public static boolean hasBody(Response response)
    {
        // HEAD requests never yield a body regardless of the response headers.
        if (response.request().method().equals("HEAD"))
        {
            return false;
        }

        int responseCode = response.code();
        if ((responseCode < HTTP_CONTINUE || responseCode >= 200)
                && responseCode != HTTP_NO_CONTENT
                && responseCode != HTTP_NOT_MODIFIED)
        {
            return true;
        }

        // If the Content-Length or Transfer-Encoding headers disagree with the
        // response code, the response is malformed. For best compatibility, we
        // honor the headers.
        if (contentLength(response) != -1
                || "chunked".equalsIgnoreCase(response.header("Transfer-Encoding")))
        {
            return true;
        }

        return false;
    }

    private static long stringToLong(String s)
    {
        if (s == null) return -1;
        try
        {
            return Long.parseLong(s);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    private boolean bodyEncoded(Headers headers)
    {
        String contentEncoding = headers.get("Content-Encoding");
        return contentEncoding != null && !contentEncoding.equalsIgnoreCase("identity");
    }

    @Override
    public Response intercept(Chain chain) throws IOException
    {
        Level level = this.level;

        Request request = chain.request();

        boolean logBody = level == Level.BODY;
        boolean logHeaders = logBody;

        RequestBody requestBody = request.body();
        boolean hasRequestBody = requestBody != null;

        Connection connection = chain.connection();
        Protocol protocol = connection != null ? connection.protocol() : Protocol.HTTP_1_1;
        String requestStartMessage = "--> " + request.method() + ' ' + request.url() + ' ' + protocol;
        if (!logHeaders && hasRequestBody)
        {
            requestStartMessage += " (" + requestBody.contentLength() + "-byte body)";
        }
        logger.log(requestStartMessage);

        if (logHeaders)
        {
            if (hasRequestBody)
            {
                // Request body headers are only present when installed as a network interceptor. Force
                // them to be included (when available) so there values are known.
                if (requestBody.contentType() != null)
                {
                    logger.log("Content-Type: " + requestBody.contentType());
                }
                if (requestBody.contentLength() != -1)
                {
                    logger.log("Content-Length: " + requestBody.contentLength());
                }
            }

            Headers headers = request.headers();
            for (int i = 0, count = headers.size(); i < count; i++)
            {
                String name = headers.name(i);
                // Skip headers from the request body as they are explicitly logged above.
                if (!"Content-Type".equalsIgnoreCase(name) && !"Content-Length".equalsIgnoreCase(name))
                {
                    logger.log(name + ": " + headers.value(i));
                }
            }

            if (!logBody || !hasRequestBody)
            {
                logger.log("--> END " + request.method());
            }
            else if (bodyEncoded(request.headers()))
            {
                logger.log("--> END " + request.method() + " (encoded body omitted)");
            }
            else
            {
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);

                Charset charset = UTF8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null)
                {
                    charset = contentType.charset(UTF8);
                }

                logger.log("");
                logger.log(buffer.readString(charset));

                logger.log("--> END " + request.method()
                        + " (" + requestBody.contentLength() + "-byte body)");
            }
        }

        long startNs = System.nanoTime();
        Response response = chain.proceed(request);
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

        ResponseBody responseBody = response.body();
        long contentLength = responseBody.contentLength();
        String bodySize = contentLength != -1 ? contentLength + "-byte" : "unknown-length";
        logger.log("<-- " + response.code() + ' ' + response.message() + ' '
                + response.request().url() + " (" + tookMs + "ms" + (!logHeaders ? ", "
                + bodySize + " body" : "") + ')');

        if (logHeaders)
        {
            Headers headers = response.headers();
            for (int i = 0, count = headers.size(); i < count; i++)
            {
                logger.log(headers.name(i) + ": " + headers.value(i));
            }

            if (!logBody || !hasBody(response))
            {
                logger.log("<-- END HTTP");
            }
            else if (bodyEncoded(response.headers()))
            {
                logger.log("<-- END HTTP (encoded body omitted)");
            }
            else
            {
                BufferedSource source = responseBody.source();
                source.request(Long.MAX_VALUE); // Buffer the entire body.
                Buffer buffer = source.buffer();

                Charset charset = UTF8;
                MediaType contentType = responseBody.contentType();
                if (contentType != null)
                {
                    try
                    {
                        charset = contentType.charset(UTF8);
                    }
                    catch (UnsupportedCharsetException e)
                    {
                        logger.log("");
                        logger.log("Couldn't decode the response body; charset is likely malformed.");
                        logger.log("<-- END HTTP");

                        return response;
                    }
                }

                if (contentLength != 0)
                {
                    logger.log("");
                    logger.log("\n" + JsonUtils.format(JsonUtils.convertUnicode(buffer.clone().readString(charset))));
                }

                logger.log("<-- END HTTP (" + buffer.size() + "-byte body)");
            }
        }

        return response;
    }

    public enum Level
    {

        /**
         * Logs request and response lines and their respective headers and bodies (if present).
         * <p/>
         * <p>Example:
         * <pre>{@code
         * --> POST /greeting http/1.1
         * Host: example.com
         * Content-Type: plain/text
         * Content-Length: 3
         *
         * Hi?
         * --> END GET
         *
         * <-- 200 OK (22ms)
         * Content-Type: plain/text
         * Content-Length: 6
         *
         * Hello!
         * <-- END HTTP
         * }</pre>
         */
        BODY
    }

    public interface Logger
    {
        /**
         * A {@link Logger} defaults output appropriate for the current platform.
         */
        Logger DEFAULT = new Logger()
        {
            @Override
            public void log(String message)
            {
                Log.d(sTag, message);
            }
        };

        void log(String message);
    }
}