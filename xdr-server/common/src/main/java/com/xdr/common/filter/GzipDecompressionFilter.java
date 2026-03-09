package com.xdr.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GzipDecompressionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String contentEncoding = request.getHeader("Content-Encoding");

        // Log to common log if possible, but for now stdout/stderr for visibility
        if (contentEncoding != null && contentEncoding.toLowerCase().contains("gzip")) {
            System.out.println("Decompressing GZIP request for URI: " + request.getRequestURI());
            filterChain.doFilter(new GzipRequestWrapper(request), response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private static class GzipRequestWrapper extends HttpServletRequestWrapper {
        private final GZIPInputStream gzipInputStream;

        public GzipRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.gzipInputStream = new GZIPInputStream(request.getInputStream());
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    try {
                        return gzipInputStream.available() == 0;
                    } catch (IOException e) {
                        return true;
                    }
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() throws IOException {
                    return gzipInputStream.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return gzipInputStream.read(b, off, len);
                }
            };
        }
    }
}
