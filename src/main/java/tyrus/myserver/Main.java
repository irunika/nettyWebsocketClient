package tyrus.myserver;

import org.glassfish.tyrus.server.Server;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.websocket.DeploymentException;

/**
 * Created by irunika on 8/29/17.
 */
public class Main {

    public static void main(String[] args) {

        Map<String, Object> props = new HashMap<String, Object>();

        Server server = new Server("localhost", 9090, "/", ServerEndpoint.class);

        try {
            server.start();
            System.out.println("Press any key to stop the server..");
            new Scanner(System.in).nextLine();
        } catch (DeploymentException e) {
            throw new RuntimeException(e);
        }
    }
}
