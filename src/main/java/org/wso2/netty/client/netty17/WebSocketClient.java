package org.wso2.netty.client.netty17;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by irunika on 2/15/17.
 */
public class WebSocketClient {
    static final String URL = System.getProperty("url", "ws://localhost:9090/basic/irunika/weeraratne");

    public static void main(String[] args) throws Exception {
        URI uri = new URI(URL);
        String scheme = uri.getScheme() == null? "ws" : uri.getScheme();
        final String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        final int port;
        if (uri.getPort() == -1) {
            if ("ws".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("wss".equalsIgnoreCase(scheme)) {
                port = 443;
            } else {
                port = -1;
            }
        } else {
            port = uri.getPort();
        }

        if (!"ws".equalsIgnoreCase(scheme) && !"wss".equalsIgnoreCase(scheme)) {
            System.err.println("Only WS(S) is supported.");
            return;
        }

        final boolean ssl = "wss".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        } else {
            sslCtx = null;
        }

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
            // If you change it to V00, ping is not supported and remember to change
            // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
            final WebSocketClientHandler handler =
                    new WebSocketClientHandler(
                            WebSocketClientHandshakerFactory
                                    .newHandshaker(uri, WebSocketVersion.V13, null, true, new DefaultHttpHeaders()));

            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            if (sslCtx != null) {
                                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
                            }
                            p.addLast(new HttpClientCodec());
                            p.addLast(new HttpObjectAggregator(65536));
                            p.addLast(WebSocketClientCompressionHandler.INSTANCE);
                            p.addLast(handler);
                        }
                    });

            Channel ch = b.connect(uri.getHost(), port).addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) throws Exception {
                    Throwable cause = future.cause();
                    if (future.isSuccess() && cause == null) {
                        handler.handshakeFuture().addListener(new ChannelFutureListener() {
                            public void operationComplete(ChannelFuture future) throws Exception {
                                Throwable cause = future.cause();
                                if (future.isSuccess() && cause == null) {
                                    System.out.println("Connection successful");
                                } else {
                                    System.out.println("Inner Connection error: " + cause.getMessage());
                                }
                            }
                        });
                    } else {
                        System.out.println("Outer Connection error: " + cause.getMessage());
                    }
                }
            }).sync().channel();

            BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String msg = console.readLine();
                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    ch.writeAndFlush(new CloseWebSocketFrame());
                    ch.closeFuture().sync();
                    break;
                } else if ("byte".equals(msg.toLowerCase())) {
                    String text = "hello world";
                    byte[] textBytes = text.getBytes();
                    ByteBuffer buffer = ByteBuffer.wrap(textBytes);
                    WebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer));
                    ch.writeAndFlush(frame);
                } else if ("pingme".equals(msg.toLowerCase())) {
                    String text = "hello world";
                    byte[] textBytes = text.getBytes();
                    ByteBuffer buffer = ByteBuffer.wrap(textBytes);
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(buffer));
                    ch.writeAndFlush(frame);
                } else if ("pong".equals(msg.toLowerCase())) {
                    String text = "hello world";
                    byte[] textBytes = text.getBytes();
                    ByteBuffer buffer = ByteBuffer.wrap(textBytes);
                    WebSocketFrame frame = new PongWebSocketFrame(Unpooled.wrappedBuffer(buffer));
                    ch.writeAndFlush(frame);
                } else if ("http".equals(msg.toLowerCase())) {
                    HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/hi");
                    ch.writeAndFlush(request);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    ch.writeAndFlush(frame);
                }
            }
        } finally {
            group.shutdownGracefully();
        }
    }
}
