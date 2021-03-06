package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.codec.http2.stream.HTTPTunnelConnection;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.TcpConnection;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.SimpleResponse;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;

import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Pengtao Qiu
 */
public class ProxyDemo {

    public static void main(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        SimpleHTTPServer server = new SimpleHTTPServer();
        SimpleTcpClient tcpClient = new SimpleTcpClient();

        server.acceptHTTPTunnelConnection((request, serverConnection) -> {
            SimpleResponse response = request.getAsyncResponse();
            request.getAttributes().computeIfAbsent("tunnelSuccess", k -> {
                Promise.Completable<TcpConnection> p = tcpClient.connect(request.getURI().getHost(), request.getURI().getPort());
                p.thenAccept(tcpConn -> {
                    Promise.Completable<HTTPTunnelConnection> promise = new Promise.Completable<>();
                    serverConnection.upgradeHTTPTunnel(promise);
                    promise.thenAccept(tunnel -> {
                        tcpConn.receive(dstBuf -> tunnel.write(dstBuf, Callback.NOOP))
                               .exception(e -> tcpConn.close())
                               .closeCallback(() -> request.remove("tunnelSuccess"));
                        tunnel.content(tcpConn::write);
                    });
                    IO.close(response);
                }).exceptionally(e -> {
                    response.setStatus(HttpStatus.BAD_GATEWAY_502);
                    IO.close(response);
                    return null;
                });
                return p;
            });
        }).headerComplete(srcRequest -> {
            long start = System.currentTimeMillis();
            System.out.println(srcRequest.toString());
            System.out.println(srcRequest.getFields());
            try {
                // copy origin request line and headers to destination request
                Promise.Completable<HTTPOutputStream> outputCompletable = new Promise.Completable<>();
                SimpleHTTPClient.RequestBuilder dstReq = client.request(srcRequest.getMethod(), srcRequest.getURI().toURI().toURL())
                                                               .addAll(srcRequest.getFields())
                                                               .output(outputCompletable);

                long contentLength = srcRequest.getContentLength();
                if (contentLength > 0) {
                    // transmit origin request body to destination server
                    AtomicLong count = new AtomicLong();
                    srcRequest.content(srcBuffer -> outputCompletable.thenAccept(dstOutput -> {
                        try {
                            if (count.addAndGet(srcBuffer.remaining()) < contentLength) {
                                dstOutput.write(srcBuffer);
                            } else {
                                dstOutput.write(srcBuffer);
                                dstOutput.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }));
                } else {
                    outputCompletable.thenAccept(IO::close);
                }

                srcRequest.messageComplete(req -> {
                    SimpleResponse srcResponse = req.getAsyncResponse();
                    dstReq.headerComplete(dstResponse -> {
                        // copy destination server response line and headers to origin response
                        System.out.println(dstResponse.toString());
                        System.out.println(dstResponse.getFields());
                        srcResponse.setStatus(dstResponse.getStatus());
                        srcResponse.setReason(dstResponse.getReason());
                        srcResponse.setHttpVersion(dstResponse.getHttpVersion());
                        srcResponse.getFields().addAll(dstResponse.getFields());
                    }).content(dstBuffer -> {
                        // transmit destination server response body
                        System.out.println("receive dst data -> " + dstBuffer.remaining());
                        try {
                            srcResponse.getOutputStream().write(BufferUtils.toArray(dstBuffer));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).messageComplete(dstResponse -> {
                        IO.close(srcResponse);
                        System.out.println("time: " + (System.currentTimeMillis() - start));
                    }).end();
                });
                System.out.println("block time: " + (System.currentTimeMillis() - start) + "|" + srcRequest.getRequest().getURIString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).listen("localhost", 3344);
    }
}
