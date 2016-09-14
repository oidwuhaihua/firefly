package test.http;

import java.io.PrintWriter;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServer.SimpleResponse;

public class SimpleHTTPServerDemo1 {

	public static void main(String[] args) {
		SimpleHTTPServer server = new SimpleHTTPServer();
		server.headerComplete(request -> {
			request.messageComplete(req -> {
				SimpleResponse response = req.getResponse();
				String path = req.getURI().getPath();
				
				response.getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
				switch (path) {
				case "/index":
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("hello index");
					}
					break;
				case "/test":
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("hello test");
					}
					break;
				default:
					try (PrintWriter writer = response.getPrintWriter()) {
						writer.print("hello world!");
					}
					break;
				}
			});
		}).listen("localhost", 3322);;

	}

}