package com.gareeva.cloudStorage.server;

import com.gareeva.cloudStorage.common.MessageFile;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class UnlimitedHandler extends ChannelInboundHandlerAdapter {
    private ByteBuf accumulator;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ByteBufAllocator allocator = ctx.alloc();
        accumulator = allocator.directBuffer(1024 * 1024 * 1, 5 * 1024 * 1024);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {

        MessageFile fm = (MessageFile) msg;

        byte[] input = fm.getData();
        accumulator.writeBytes(input);
        String fileName = getFileName(fm.getLogin(), fm.getFilename());
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(fileName, true))) {
            while (accumulator.readableBytes() > 0) {
                out.write(accumulator.readByte());
            }
            accumulator.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFileName(String login, String fileName) {
        return Constants.SERVER_STORAGE + login + "/" + fileName;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
