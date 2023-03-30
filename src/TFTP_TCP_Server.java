import java.net.*;

/**
 * A simple TFTP server that uses TCP
 */
public class TFTP_TCP_Server {
    public static void main(String[] args) throws Exception {
        // Create a TCP socket for the server
        ServerSocket serverSocket = new ServerSocket(69);

        while (true) {
            Socket clientSocket = null;
            try {
                // Wait for a client to connect
                new TFTP_TCP_ServerRequestHandler(serverSocket.accept()).start();
            } catch (Exception e) {
                System.err.println("Error handling client request: " + e.getMessage());
                if (clientSocket != null) {
                    clientSocket.close();
                }
            }
        }
    }
}
