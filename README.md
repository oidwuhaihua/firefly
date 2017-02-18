# What is Firefly?

Firefly framework is a asynchronous java web framework. It helps you create a web application ***Easy*** and ***Quickly***. It provides MVC framework, asynchronous HTTP Server/Client, asynchronous TCP Server/Client and many other useful components for developing web applications, protocol servers, etc. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using ***SEDA*** architecture, a highly customizable thread model.  

Firefly core provides functionality for things like:
- Writing TCP clients and servers
- Writing HTTP clients and servers
- Writing web application with MVC framework and template engine
- Database access

# Event driven

The Firefly APIs are largely event driven. This means that when things happen in Firefly that you are interested in, Firefly will call you by sending you events.

Some example events are:
- some data has arrived on a socket
- an HTTP server has received a request

Firefly handles a lot of concurrency using just a small number of threads, so ***don't block Firefly thread***, you must manage blocking call in the standalone thread pool.  

With a conventional blocking API the calling thread might block when:
- Thread.sleep()
- Waiting on a lock
- Waiting on a mutex or monitor
- Doing a long lived database operation and waiting for a result
- Call blocking I/O APIs

In all the above cases, when your thread is waiting for a result it can’t do anything else - it’s effectively useless.

This means that if you want a lot of concurrency using blocking APIs then you need a lot of threads to prevent your application grinding to a halt.

Threads have overhead in terms of the memory they require (e.g. for their stack) and in context switching.

For the levels of concurrency required in many modern applications, a blocking approach just doesn’t scale.

# Quick start

Add maven dependency in your pom.xml.
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly</artifactId>
    <version>4.0.21</version>
</dependency>

<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-slf4j</artifactId>
    <version>4.0.21</version>
</dependency>
```

Create a HTTP server in main function
```java
public class HelloHTTPServer {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }
}
```

Run and view

```
http://localhost:8080/
```
More detailed information, please refer to the [full document](http://www.fireflysource.com)

#Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579