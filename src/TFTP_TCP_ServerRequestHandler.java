import java.io.*;
import java.net.*;

/**
 * A simple TFTP server that uses TCP, extends thread
 */
class TFTP_TCP_ServerRequestHandler extends Thread {
    private final Socket clientSocket;

    public TFTP_TCP_ServerRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    /**
     * Handles a client request
     */

    public void run() {
        try {
            //sets the size of the buffer to 512, similar to task1
            byte[] buffer = new byte[512];
            // Create input and output streams for the client socket
            InputStream inputStream = clientSocket.getInputStream();
            OutputStream outputStream = clientSocket.getOutputStream();
            // Read the client's request
            byte[] request = new byte[1024];
            int bytesRead = inputStream.read(request);
            // If the client closes the connection, bytesRead will be -1
            if (bytesRead == -1) {
                return;
            }
            String requestString = new String(request, 0, bytesRead);
            System.out.println("Client request:" + requestString);

            // Process the client's request
            if (requestString.startsWith("READ")) {
                String filename = requestString.substring(4).trim();
                File file = new File(filename);
                if (!file.exists()) {
                    outputStream.write("ERROR: File not found".getBytes());
                    return;
                }
                //writes into the socket output stream in 512 chunks the file input stream
                FileInputStream fis = new FileInputStream(file);
                while (true) {
                    int count = fis.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    outputStream.write(buffer, 0, count);
                }
                fis.close();
            }
            //writes the file to the socket output stream in 512 chunks
            else if (requestString.startsWith("WRITE")) {
                // Handle a TFTP write request
                String filename = requestString.substring(4).trim();
                File file = new File(filename);
                //writes into the file output stream in 512 chunks the socket input stream
                FileOutputStream fos = new FileOutputStream(file);
                while (true) {
                    int count = inputStream.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    fos.write(buffer, 0, count);
                }
                fos.close();
            }
            clientSocket.close();
        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }
}