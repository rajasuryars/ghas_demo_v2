import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class SocketConnection {
    Socket other_client;
    String host_id;
    String remote_id;
    BufferedReader in;
    PrintWriter out;
    Boolean initiator;
    Client master;
    public SocketConnection(Socket other_client, String host_id, Boolean initiator, Client master) {
        this.other_client = other_client;
        this.host_id = host_id;
        this.master = master;
        try {
            in = new BufferedReader(new InputStreamReader(this.other_client.getInputStream()));
            out = new PrintWriter(this.other_client.getOutputStream(), true);
        } catch (Exception e) {
            System.out.println("SocketConnection Exception: " + e);
        }
        try {
            if (!initiator) {
                out.println("SEND_ID");
                System.out.println("SEND_ID request sent");
                remote_id = in.readLine();
                System.out.println("SEND_ID request response received with ID: " + remote_id);
            }
        } catch (Exception e) {
            System.out.println("SocketConnection constructor exception: " + e);
        }
        Thread read = new Thread() {
            public void run() {
                while(rx_cmd(in, out) != 0) {}
            }
        };
        read.setDaemon(true);   // terminate when main ends
        read.start();
    }
    public int rx_cmd(BufferedReader cmd, PrintWriter out) {
        try {
            String cmd_in = cmd.readLine();
            if (cmd_in.equals("P")) {
                System.out.println("Received command: P");
            } else if (cmd_in.equals("SEND_ID")) {
                System.out.println("Received command: SEND_ID");
                out.println(this.host_id);
            } else if (cmd_in.equals("SEND_CLIENT_ID")) {
                System.out.println("Received command: SEND_CLIENT_ID");
                out.println(this.host_id);
            } else if (cmd_in.equals("REQ")) {
                System.out.println("Received command: REQ");
                String req_client_id = cmd.readLine();
                Integer req_client_logical_clock = Integer.valueOf(cmd.readLine());
                String file_name = cmd.readLine();
                System.out.println("Received request from client: " + req_client_id + " which had logical clock value of: " + req_client_logical_clock);
                System.out.println("Calling client: " + this.host_id + "'s request processor");
                master.processRequest(req_client_id, req_client_logical_clock, file_name);
            } else if (cmd_in.equals("REP")) {
                System.out.println("Received command: REP");
                String rep_client_id = cmd.readLine();
                String file_name = cmd.readLine();
                System.out.println("Received reply from client: " + rep_client_id);
                master.processReply(rep_client_id, file_name);
            } else if (cmd_in.equals("READ_FROM_FILE_ACK")) {
                System.out.println("Received command: READ_FROM_FILE_ACK");
                String res_server_id = cmd.readLine();
                String file_name = cmd.readLine();
                String last_message_client = cmd.readLine();
                String last_message_timestamp = cmd.readLine();
                master.fileReadAckProcessor(res_server_id, file_name, new Message(last_message_client, last_message_timestamp));
            } else if (cmd_in.equals("WRITE_TO_FILE_ACK")) {
                System.out.println("Received command: WRITE_TO_FILE_ACK");
                String file_name = cmd.readLine();
                master.processWriteAck(file_name);
            } else if (cmd_in.equals("ENQUIRE_ACK")) {
                System.out.println("Received command: ENQUIRE_ACK");
                String all_files = cmd.readLine();
                master.setHostedFiles(all_files);
            }
        } catch(Exception e) {
            System.out.println("rx_cmd exception: " + e);
        }
        return 1;
    }
    public String get_remote_id() {
        return remote_id;
    }
    public void set_remote_id(String remote_id) {
        this.remote_id = remote_id;
    }
    public synchronized void publish() {
        out.println("P");
    }
    public synchronized void serverWriteTest() {
        out.println("WRITE_TEST");
    }
    public synchronized void write(String file_name, Message message) {
        System.out.println("Sending write request from Client ID: " + this.host_id + " to server with Server ID: " + this.get_remote_id());
        out.println("WRITE_TO_FILE");
        out.println(file_name);
        out.println(message.client_id);
        out.println(message.timestamp);
    }
    public synchronized void read(String file_name) {
        System.out.println("Sending read request from Client ID: " + this.host_id + " to server with Server ID: " + this.get_remote_id());
        out.println("READ_FROM_FILE");
        out.println(file_name);
        out.println(this.host_id);
    }
    public synchronized void request(Integer logical_clock, String file_name) {
        System.out.println("Sending request from Client ID: " + this.host_id + " to remote Client ID: " + this.get_remote_id() + " for file: " + file_name);
        out.println("REQ");
        out.println(this.host_id);
        out.println(logical_clock);
        out.println(file_name);
    }
    public synchronized void reply(String file_name) {
        System.out.println("Sending reply from Client ID: " + this.host_id + " to remote Client ID: " + this.get_remote_id() + " for file: " + file_name);
        out.println("REP");
        out.println(this.host_id);
        out.println(file_name);
    }
    public synchronized void sendEnquire() {
        System.out.println("Sending Enquire to Server");
        out.println("ENQUIRE");
        out.println(this.host_id);
    }
    public Socket get_other_client() {
        return other_client;
    }
    public void set_other_client(Socket other_client) {
        this.other_client = other_client;
    }
}