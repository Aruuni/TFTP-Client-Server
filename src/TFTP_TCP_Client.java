import java.io.*;
import java.net.*;

/**
 * A simple TFTP client that uses TCP sockets.
 */
public class TFTP_TCP_Client{
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || args.length > 4) {
            System.out.println("Usage: java TFTPClient <server> <read|write> <filename>");
            System.exit(1);
        }
        String filename = args[2];
        // Set up a socket to connect to the server
        Socket socket = new Socket(args[0], 69);
        // Create input and output streams for the socket
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        // Send the request to the server
        out.write((args[1].toUpperCase() + " " + filename).getBytes());

        // handles a TFTP read request
        if (args[1].equalsIgnoreCase("READ")) {
            // Opens a file for writing
            File file = new File(filename);
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[512];
            // Read the file from the socket
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
            System.out.println("File saved as: " + filename);
        }
        else if (args[1].equalsIgnoreCase("WRITE")) {
            // Handle a TFTP write request
            File file = new File(filename);
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[512];
            int bytesRead;
            //writes the file to the socket output stream in 512 chunks
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            fis.close();
            System.out.println("File sent successfully");
        }
        else {
            System.out.println("Invalid request");
        }
        // Close the socket
        socket.close();
        System.out.println("file transfer complete");
        System.exit(0);
    }
}