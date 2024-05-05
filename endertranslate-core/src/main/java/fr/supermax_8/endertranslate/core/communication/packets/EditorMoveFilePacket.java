package fr.supermax_8.endertranslate.core.communication.packets;

import fr.supermax_8.endertranslate.core.communication.ServerWebSocketClient;
import fr.supermax_8.endertranslate.core.communication.WebSocketServer;
import fr.supermax_8.endertranslate.core.communication.WsPacket;
import fr.supermax_8.endertranslate.core.translation.TranslationManager;
import org.eclipse.jetty.websocket.api.Session;

import java.io.File;

public class EditorMoveFilePacket implements WsPacket {

    private String fromPath;
    private String toPath;

    @Override
    public void receiveFromClient(Session jettySession, WebSocketServer socket) {
        TranslationManager.getInstance().moveFile(fromPath.replaceAll("/", File.separator), toPath.replaceAll("/", File.separator));
    }

    @Override
    public void receiveFromServer(ServerWebSocketClient ws) {

    }

}