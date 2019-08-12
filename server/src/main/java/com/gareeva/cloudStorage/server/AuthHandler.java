package com.gareeva.cloudStorage.server;

import com.gareeva.cloudStorage.common.*;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.sql.SQLException;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RequestAuth) {
            //здесь обрабатывается запрос пользователя на аутентификацию
            RequestAuth ra = (RequestAuth) msg;
            boolean authSuccess = JdbcClass.getInstance().authUser(ra.getLogin(), ra.getPassword());
            ctx.writeAndFlush(new ReportAuth(authSuccess));

        } else if (msg instanceof RequestRegistration) {
            RequestRegistration rr = (RequestRegistration) msg;
            String login = rr.getLogin();
            String password = rr.getPassword();
            boolean userIsCreated = createNewUser(login, password);
            ctx.writeAndFlush(new ReportRegistration(userIsCreated));
        } else {
            AbstractMessage message = (AbstractMessage) msg;
            if (message.getLogin() != null) {
                //считаем, что в сообщениях от авторизованного пользователя заполнено поле login и этого достаточно
                ctx.fireChannelRead(message);
            }
        }
    }

    private void createNewFolder(String login) {
        File file = new File(Constants.SERVER_STORAGE + login);
        file.mkdir();

    }

    private boolean createNewUser(String login, String password) {
        try {
            JdbcClass.getInstance().createUser(login, password);
            createNewFolder(login);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
