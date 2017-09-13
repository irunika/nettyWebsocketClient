package tyrus.myserver;

import javax.websocket.OnOpen;
import javax.websocket.Session;

@javax.websocket.server.ServerEndpoint(value = "/chat")
public class ServerEndpoint {

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("New connection");
    }
}
