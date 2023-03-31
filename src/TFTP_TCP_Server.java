import java.net.*;
/**
 * A simple TFTP server that uses TCP
 */
public class TFTP_TCP_Server {
    public static void main(String[] args) throws Exception {
        while (true) {
            try (ServerSocket serverSocket = new ServerSocket(69)){
                // Wait for a client to connect
                new TFTP_TCP_ServerRequestHandler(serverSocket.accept()).start();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }
}
