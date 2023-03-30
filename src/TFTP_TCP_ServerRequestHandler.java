import java.io.*;
import java.net.*;

//class TFTP_TCP_ServerRequest extends Thread {
class TFTP_TCP_ServerRequestHandler extends Thread {
    private final Socket clientSocket;

    public TFTP_TCP_ServerRequestHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    public void run() {
        try {
            // Create input and output streams for the client socket
            BufferedInputStream in = new BufferedInputStream(clientSocket.getInputStream());
            OutputStream out = clientSocket.getOutputStream();

            // Read the client's request
            byte[] request = new byte[1024];
            int bytesRead = in.read(request);
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
                    out.write("ERROR: File not found".getBytes());
                    return;
                }
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[512];
                while (true) {
                    int count = fis.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    out.write(buffer, 0, count);
                }
                fis.close();
            } else if (requestString.startsWith("WRITE")) {
                // Handle a TFTP write request
                String filename = requestString.substring(4).trim();
                File file = new File(filename);
                FileOutputStream fos = new FileOutputStream(file+".jpg"+".jpg");
                byte[] buffer = new byte[512];
                while (true) {
                    int count = in.read(buffer);
                    if (count == -1) {
                        break;
                    }
                    fos.write(buffer, 0, count);
                }
                fos.close();
                out.write("File saved successfully".getBytes());
            } else {
                out.write("ERROR: Invalid request".getBytes());
            }
        } catch (Exception e) {
            System.err.println("Error handling client request: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}