/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.netty.client.netty17.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Echoes uppercase content of text frames.
 */
public class WebSocketFrameHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

//    private static final Logger logger = LoggerFactory.getLogger(WebSocketFrameHandler.class);
    private static AtomicLong atomicLongMessages = new AtomicLong(0);
    private static AtomicLong atomicLongConnections = new AtomicLong(0);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Connection: " + atomicLongConnections.incrementAndGet());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("channel is inactive");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        // ping and pong frames already handled

        System.out.println("Message: " + atomicLongMessages.incrementAndGet());

        if (frame instanceof TextWebSocketFrame) {
            // Send the uppercase string back.
            String request = ((TextWebSocketFrame) frame).text();
            System.out.println("received " + ctx.channel() + ": " + request);
            if ("ping".equals(request)) {
                ctx.channel().writeAndFlush(new PingWebSocketFrame(Unpooled.wrappedBuffer(ByteBuffer.wrap(new byte[]{1,2,3,4}))));
                return;
            }
            if ("http".equals(request)) {
                HttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "hi");
                ctx.writeAndFlush(httpRequest);
                return;
            }
            ctx.channel().writeAndFlush(new TextWebSocketFrame(request.toUpperCase(Locale.US)));
        } else if (frame instanceof BinaryWebSocketFrame) {
            System.out.println("received " + ctx.channel() + ": " + "bytes");
            WebSocketFrame frame1 = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(frame.content().nioBuffer()));
            ctx.channel().writeAndFlush(frame1);
        } else if (frame instanceof PingWebSocketFrame) {
            System.out.println("WebSocket server received ping");
            frame.retain();
            ctx.channel().writeAndFlush(new PingWebSocketFrame(frame.content()));
//            frame.retain();
//            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
        } else if (frame instanceof PongWebSocketFrame) {
            System.out.println("WebSocket server received pong");
            frame.retain();
            ctx.channel().writeAndFlush(new PongWebSocketFrame(frame.content()));
        } else if (frame instanceof CloseWebSocketFrame) {
            System.out.println("Connection is closed.");
            ctx.close();
        }else {
            String message = "unsupported frame type: " + frame.getClass().getName();
            throw new UnsupportedOperationException(message);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    }
}
