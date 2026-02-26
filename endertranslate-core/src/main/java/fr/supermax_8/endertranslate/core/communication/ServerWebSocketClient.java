package fr.supermax_8.endertranslate.core.communication;

import com.google.gson.JsonSyntaxException;
import fr.supermax_8.endertranslate.core.EnderTranslate;
import fr.supermax_8.endertranslate.core.EnderTranslateConfig;
import fr.supermax_8.endertranslate.core.communication.packets.ServerAuthPacket;
import fr.supermax_8.endertranslate.core.utils.WebSocketClient;
import lombok.Getter;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;

public class ServerWebSocketClient extends WebSocketClient {

    @Getter
    private static ServerWebSocketClient instance;

    private final StringBuilder textBuffer = new StringBuilder();

    public ServerWebSocketClient(String wsUrl) {
        super(wsUrl);
        start();
        instance = this;
    }

    @Override
    protected void onOpen(WebSocket webSocket) {
        super.onOpen(webSocket);
        sendPacket(new ServerAuthPacket(EnderTranslateConfig.getInstance().getSecret()));
    }

    @Override
    protected void onText(String receivedText, boolean last) {
        textBuffer.append(receivedText);

        if (!last) return;

        String fullMessage = textBuffer.toString();
        textBuffer.setLength(0);

        try {
            WsPacketWrapper packet = EnderTranslate.getGson().fromJson(fullMessage, WsPacketWrapper.class);
            packet.getPacket().receiveFromServer(this);
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onBinary(ByteBuffer receivedBinary, boolean last) {
    }

    public void sendPacket(WsPacket packet) {
        try {
            sendMessage(EnderTranslate.getGson().toJson(new WsPacketWrapper(packet)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}