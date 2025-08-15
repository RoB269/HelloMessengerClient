import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.*;

//Client
public class Test {
    public static DataOutputStream dos;
    public static DataInputStream dis;
    public static void main(String[] args) {
        try (Socket server = new Socket("127.0.0.1", 12234)) {
            dos = new DataOutputStream(server.getOutputStream());
            dis = new DataInputStream(server.getInputStream());
            server.setKeepAlive(true);
            SideServerThread thread = new SideServerThread(server);
            thread.start();
            while (true){
                Scanner scanner = new Scanner(System.in);
                dos.writeUTF(scanner.nextLine());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class SideServerThread extends Thread {
    Socket client;

    public SideServerThread(Socket client) {
        this.client = client;
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.println(Test.dis.readUTF());
            }
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }
}