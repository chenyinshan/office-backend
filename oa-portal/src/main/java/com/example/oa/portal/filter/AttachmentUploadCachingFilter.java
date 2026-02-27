package com.example.oa.portal.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;

/**
 * 对附件上传请求缓存原始 body，便于 portal 透传带 boundary 的 multipart 到 workflow-service。
 * 若不缓存，Spring 会先解析 multipart，portal 再用 RestTemplate 重组时容易丢失 boundary。
 */
public class AttachmentUploadCachingFilter implements Filter {

    public static final String ATTRIBUTE_CACHED_BODY = "oa.portal.cachedBody";
    public static final String ATTRIBUTE_CONTENT_TYPE = "oa.portal.contentType";
    private static final String URI = "/api/workflow/attachments/upload";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!(request instanceof HttpServletRequest)) {
            chain.doFilter(request, response);
            return;
        }
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        if (!"POST".equalsIgnoreCase(req.getMethod()) || (uri == null || !uri.endsWith("attachments/upload"))) {
            chain.doFilter(request, response);
            return;
        }
        String contentType = req.getContentType();
        byte[] body = readFully(req.getInputStream());
        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(req, body, contentType);
        chain.doFilter(wrapper, response);
    }

    private static byte[] readFully(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    public static byte[] getCachedBody(HttpServletRequest request) {
        CachedBodyRequestWrapper w = findWrapper(request);
        return w != null ? w.getCachedBody() : (byte[]) request.getAttribute(ATTRIBUTE_CACHED_BODY);
    }

    public static String getCachedContentType(HttpServletRequest request) {
        CachedBodyRequestWrapper w = findWrapper(request);
        return w != null ? w.getCachedContentType() : (String) request.getAttribute(ATTRIBUTE_CONTENT_TYPE);
    }

    private static CachedBodyRequestWrapper findWrapper(HttpServletRequest request) {
        HttpServletRequest r = request;
        while (r != null) {
            if (r instanceof CachedBodyRequestWrapper) return (CachedBodyRequestWrapper) r;
            r = r instanceof HttpServletRequestWrapper ? (HttpServletRequest) ((HttpServletRequestWrapper) r).getRequest() : null;
        }
        return null;
    }

    /** 包装请求并持有 body/contentType，便于下游通过 getAttribute 或静态方法获取 */
    public static final class CachedBodyRequestWrapper extends HttpServletRequestWrapper {
        private final byte[] body;
        private final String contentType;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body, String contentType) {
            super(request);
            this.body = body;
            this.contentType = contentType;
        }

        byte[] getCachedBody() {
            return body;
        }

        String getCachedContentType() {
            return contentType;
        }

        @Override
        public Object getAttribute(String name) {
            if (ATTRIBUTE_CACHED_BODY.equals(name)) return body;
            if (ATTRIBUTE_CONTENT_TYPE.equals(name)) return contentType;
            return super.getAttribute(name);
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private int i;

                @Override
                public boolean isFinished() {
                    return i >= body.length;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener listener) {}

                @Override
                public int read() {
                    return i < body.length ? (body[i++] & 0xff) : -1;
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(),
                    getCharacterEncoding() != null ? java.nio.charset.Charset.forName(getCharacterEncoding()) : java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
