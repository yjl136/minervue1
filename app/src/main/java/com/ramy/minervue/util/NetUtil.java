package com.ramy.minervue.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * Created by peter on 12/23/13.
 */
public class NetUtil {

    public static ServerSocketChannel openServerSocketChannel(int port) {
        ServerSocketChannel socket;
        try {
            socket = ServerSocketChannel.open();
        } catch (IOException e) {
            return null;
        }
        try {
            socket.socket().bind(new InetSocketAddress(port));
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ex) {
                // Ignored.
            }
            socket = null;
        }
        return socket;
    }

    public static void cleanSocket(ServerSocketChannel socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignored.
        }
    }

    public static void cleanSocket(SocketChannel socket) {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            // Ignored.
        }
    }

}
