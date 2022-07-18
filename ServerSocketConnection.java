import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ServerSocketConnection {
    Socket other_client;
    String host_id;
    String remote_id;
    BufferedReader in;
    PrintWriter out;
    Boolean initiator;
    Server master;
    public ServerSocketConnection(Socket other_client, String host_id, Boolean is_server, Server master) {
        this.other_client = other_client;
        this.host_id = host_id;
        this.master = master;
        try {
            in = new BufferedReader(new InputStreamReader(this.other_client.getInputStream()));
            out = new PrintWriter(this.other_client.getOutputStream(), true);
        } catch (Exception e) {
            System.out.println("ServerSocketConnection Exception: " + e);
        }
        try {
            if (!is_server) {
                out.println("SEND_CLIENT_ID");
                System.out.println("SEND_CLIENT_ID request sent");
                remote_id = in.readLine();
                System.out.println("SEND_CLIENT_ID request response received with ID: " + remote_id);
            }
        } catch (Exception e) {
            System.out.println("ServerSocketConnection constructor exception: " + e);
        }
        Thread read = new Thread() {
            public void run() {
                while(rx_cmd(in, out) != 0) {}
            }
        };
        read.setDaemon(true);   // terminate when main ends
        read.start();
    }
    public String get_remote_id() {
        return remote_id;
    }
    public void set_remote_id(String remote_id) {
        this.remote_id = remote_id;
    }
    public int rx_cmd(BufferedReader cmd, PrintWriter out) {
        try {
            String cmd_in = cmd.readLine();
            if (cmd_in.equals("WRITE_TEST")) {
                System.out.println("Received command: WRITE_TEST");
            } else if (cmd_in.equals("WRITE_TO_FILE")) {
                System.out.println("Received command: WRITE_TO_FILE");
                String file_name = cmd.readLine();
                String req_client_id = cmd.readLine();
                String req_client_timestamp = cmd.readLine();
                master.writeToFile(file_name, new Message(req_client_id, req_client_timestamp));
            } else if (cmd_in.equals("READ_FROM_FILE")) {
                System.out.println("Received command: READ_FROM_FILE");
                String file_name = cmd.readLine();
                String req_client_id = cmd.readLine();
                master.readLastFile(file_name, req_client_id);
            } else if (cmd_in.equals("ENQUIRE")) {
                System.out.println("Received command: ENQUIRE");
                String req_client_id = cmd.readLine();
                master.fileHostedString(req_client_id);
            }
        } catch(Exception e) {
            System.out.println("ServerSocketConnection rx_cmd exception: " + e);
        }
        return 1;
    }
    public synchronized void publish() {
        out.println("P");
    }
    public synchronized void sendLastMessageOnFile(String file_name, Message last_message) {
        out.println("READ_FROM_FILE_ACK");
        out.println(this.host_id);
        out.println(file_name);
        out.println(last_message.get_client_id());
        out.println(last_message.get_timestamp());
    }
    public synchronized void sendWriteAck(String file_name) {
        System.out.println("Sending Write ACK: " + file_name);
        out.println("WRITE_TO_FILE_ACK");
        out.println(file_name);
    }
    public synchronized void sendHostedFiles(String hosted_files) {
        System.out.println("Sending hosted file info");
        out.println("ENQUIRE_ACK");
        out.println(hosted_files);
    }
    public Socket get_other_client() {
        return other_client;
    }
    public void set_other_client(Socket other_client) {
        this.other_client = other_client;
    }
}