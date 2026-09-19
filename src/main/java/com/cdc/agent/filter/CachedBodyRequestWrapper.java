package com.cdc.agent.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 可重复读取 Body 的 Request 包装器
 * 用于 Filter 中读取请求体后，后续 Controller 仍可正常读取
 */
public class CachedBodyRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = readBody(request);
    }

    public String getCachedBody() {
        return new String(cachedBody, StandardCharsets.UTF_8);
    }

    @Override
    public ServletInputStream getInputStream() {
        return new CachedBodyServletInputStream(cachedBody);
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), StandardCharsets.UTF_8));
    }

    private byte[] readBody(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ServletInputStream is = request.getInputStream()) {
            byte[] data = new byte[1024];
            int len;
            while ((len = is.read(data)) != -1) {
                buffer.write(data, 0, len);
            }
        }
        return buffer.toByteArray();
    }

    private static class CachedBodyServletInputStream extends ServletInputStream {

        private final byte[] body;
        private int cursor = 0;

        CachedBodyServletInputStream(byte[] body) {
            this.body = body;
        }

        @Override
        public boolean isFinished() {
            return cursor >= body.length;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setReadListener(ReadListener listener) {
            // no-op
        }

        @Override
        public int read() {
            if (cursor >= body.length) {
                return -1;
            }
            return body[cursor++] & 0xFF;
        }
    }
}
