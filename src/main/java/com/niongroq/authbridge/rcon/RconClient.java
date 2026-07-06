package com.niongroq.authbridge.rcon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal Minecraft RCON client.
 *
 * Protocol (all integers little-endian):
 *   Packet: [length:4][requestId:4][type:4][payload:n][0x00 0x00]
 *   Auth type  = 3
 *   Command type = 2
 *   Auth failure → server replies with requestId = -1
 */
public class RconClient implements AutoCloseable {

    private static final int TYPE_AUTH    = 3;
    private static final int TYPE_COMMAND = 2;
    private static final int REQUEST_ID   = 1;

    private Socket           socket;
    private DataOutputStream out;
    private DataInputStream  in;

    public synchronized void connect(String host, int port, String password) throws IOException {
        socket = new Socket(host, port);
        socket.setSoTimeout(5_000);
        out = new DataOutputStream(socket.getOutputStream());
        in  = new DataInputStream(socket.getInputStream());

        send(REQUEST_ID, TYPE_AUTH, password);
        Packet response = read();
        if (response.requestId() == -1) {
            throw new IOException("RCON authentication failed — check your rcon.password");
        }
    }

    public synchronized String execute(String command) throws IOException {
        send(REQUEST_ID, TYPE_COMMAND, command);
        return read().payload();
    }

    private void send(int requestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int packetLen = 4 + 4 + payloadBytes.length + 2; // requestId+type+payload+2 nulls

        ByteBuffer buf = ByteBuffer.allocate(4 + packetLen);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(packetLen);
        buf.putInt(requestId);
        buf.putInt(type);
        buf.put(payloadBytes);
        buf.put((byte) 0);
        buf.put((byte) 0);

        out.write(buf.array());
        out.flush();
    }

    private Packet read() throws IOException {
        int length = Integer.reverseBytes(in.readInt());
        byte[] data = new byte[length];
        in.readFully(data);

        ByteBuffer buf = ByteBuffer.wrap(data);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int requestId    = buf.getInt();
        int type         = buf.getInt();
        int payloadLen   = length - 4 - 4 - 2; // subtract requestId, type, 2 null bytes
        byte[] payloadB  = new byte[Math.max(0, payloadLen)];
        if (payloadB.length > 0) buf.get(payloadB);

        return new Packet(requestId, type, new String(payloadB, StandardCharsets.UTF_8));
    }

    @Override
    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private record Packet(int requestId, int type, String payload) {}
}
