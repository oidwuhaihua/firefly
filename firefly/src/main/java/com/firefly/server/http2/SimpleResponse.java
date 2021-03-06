package com.firefly.server.http2;

import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.BufferedHTTPOutputStream;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;

public class SimpleResponse implements Closeable {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    Response response;
    HTTPOutputStream output;
    PrintWriter printWriter;
    BufferedHTTPOutputStream bufferedOutputStream;
    int bufferSize = 8 * 1024;
    String characterEncoding = "UTF-8";
    boolean asynchronous;

    public SimpleResponse(Response response, HTTPOutputStream output) {
        this.output = output;
        this.response = response;
    }

    public HttpVersion getHttpVersion() {
        return response.getHttpVersion();
    }

    public void setHttpVersion(HttpVersion httpVersion) {
        response.setHttpVersion(httpVersion);
    }

    public HttpFields getFields() {
        return response.getFields();
    }

    public long getContentLength() {
        return response.getContentLength();
    }

    public Iterator<HttpField> iterator() {
        return response.iterator();
    }

    public int getStatus() {
        return response.getStatus();
    }

    public String getReason() {
        return response.getReason();
    }

    public void setStatus(int status) {
        response.setStatus(status);
    }

    public void setReason(String reason) {
        response.setReason(reason);
    }

    public void forEach(Consumer<? super HttpField> action) {
        response.forEach(action);
    }

    public Spliterator<HttpField> spliterator() {
        return response.spliterator();
    }

    public Response getResponse() {
        return response;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public void addCookie(Cookie cookie) {
        response.getFields().add(HttpHeader.SET_COOKIE, CookieGenerator.generateSetCookie(cookie));
    }

    public synchronized OutputStream getOutputStream() {
        if (printWriter != null) {
            throw new IllegalStateException("the response has used print writer");
        }

        if (bufferedOutputStream == null) {
            bufferedOutputStream = new BufferedHTTPOutputStream(output, bufferSize);
            return bufferedOutputStream;
        } else {
            return bufferedOutputStream;
        }
    }

    public synchronized PrintWriter getPrintWriter() {
        if (bufferedOutputStream != null) {
            throw new IllegalStateException("the response has used output stream");
        }
        if (printWriter == null) {
            try {
                printWriter = new PrintWriter(new OutputStreamWriter(new BufferedHTTPOutputStream(output, bufferSize), characterEncoding));
            } catch (UnsupportedEncodingException e) {
                log.error("create print writer exception", e);
            }
            return printWriter;
        } else {
            return printWriter;
        }
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isClosed() {
        return output.isClosed();
    }

    public synchronized void close() throws IOException {
        if (bufferedOutputStream != null) {
            bufferedOutputStream.close();
        } else if (printWriter != null) {
            printWriter.close();
        } else {
            getOutputStream().close();
        }
    }

    public synchronized void flush() throws IOException {
        if (bufferedOutputStream != null) {
            bufferedOutputStream.flush();
        } else if (printWriter != null) {
            printWriter.flush();
        }
    }

}
