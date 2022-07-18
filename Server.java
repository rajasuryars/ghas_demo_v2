import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server {
    List<Node> all_server_nodes = new LinkedList<>();
    List<ServerSocketConnection> serverSocketConnectionList = new LinkedList<>();
    ServerSocket server;
    String id;
    HashMap<String, ServerSocketConnection> serverSocketConnectionHashMap = new HashMap<>();
    HashMap<String, String> serverAndWorkFolder = new HashMap<>();
    File[] list_of_files;
    String all_files = "";
    public String get_id() {
        return id;
    }
    public List<Node> get_all_server_nodes() {
        return all_server_nodes;
    }
    public void set_id(String id) {
        this.id = id;
    }
    public void set_all_server_nodes(List<Node> all_server_nodes) {
        this.all_server_nodes = all_server_nodes;
    }
    public class CommandParser extends Thread {
        Server current_server;
        public CommandParser(Server current_server) {
            this.current_server = current_server;
        }
        Pattern START = Pattern.compile("^START$");
        Pattern SHOW_FILES = Pattern.compile("^SHOW_FILES$");
        int rx_cmd(Scanner cmd) {
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
            Matcher m_START = START.matcher(cmd_in);
            Matcher m_SHOW_FILES = SHOW_FILES.matcher(cmd_in);
            if (m_START.find()) {
                System.out.println("Socket connection test function");
                try {
                    System.out.println("STATUS UP");
                } catch (Exception e) {
                    System.out.println("CommandParser exception: " + e);
                }
            } else if (m_SHOW_FILES.find()) {
                current_server.fileHostedString("0");
            }
            return 1;
        }
        public void run() {
            System.out.println("Enter START command to set-up MESH connection: ");
            Scanner input = new Scanner(System.in);
            while (rx_cmd(input) != 0) {}
        }
    }
    public synchronized void writeToFile(String file_name, Message message) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./" + this.serverAndWorkFolder.get(this.get_id()) + file_name, true));
        writer.append(message.get_client_id() + "," + message.get_timestamp() + "\n");
        writer.close();
        serverSocketConnectionHashMap.get(message.get_client_id()).sendWriteAck(file_name);
    }
    public synchronized void fileHostedString(String req_client_id) {
        if (all_files.isEmpty()) {
            File folder = new File("./" + this.serverAndWorkFolder.get(this.get_id()) + "/");
            list_of_files = folder.listFiles();
            for (int file_index = 0; file_index < list_of_files.length; file_index++) {
                this.all_files += list_of_files[file_index].getName().substring(0, list_of_files[file_index].getName().lastIndexOf("."));
            }
        }
        serverSocketConnectionHashMap.get(req_client_id).sendHostedFiles(this.all_files);
    }
    public synchronized void readLastFile(String file_name, String req_client_id) {
        String current_line;
        String last_line = "";
        try {
            BufferedReader br = new BufferedReader(new FileReader("./" + this.serverAndWorkFolder.get(this.get_id()) + "/" + file_name));
            while ((current_line = br.readLine()) != null) {
                last_line = current_line;
            }
            br.close();
        } catch (Exception e) {
            System.out.println("readLastFile exception: " + e);
        }
        Message return_message;
        if (!last_line.isEmpty()) {
            List<String> message = Arrays.asList(last_line.split(","));
            System.out.println("Returning last line read as Message");
            return_message = new Message(message.get(0), message.get(1));
        } else {
            return_message = new Message("EMPTY FILE - NO CLIENT ID", "EMPTY FILE - NO TIMESTAMP");
        }
        serverSocketConnectionHashMap.get(req_client_id).sendLastMessageOnFile(file_name, return_message);
    }
    public void setServerWorkFolder() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("serverWorkFolder.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server_workFolder = Arrays.asList(line.split(","));
                    this.serverAndWorkFolder.put(parsed_server_workFolder.get(0), parsed_server_workFolder.get(1));
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.get_all_server_nodes().size());
            } finally {
                br.close();
            }
        } catch (Exception e) {
            System.out.println("setServerWorkFolder exception: " + e);
        }
    }
    public void setServerList() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("ServerAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_server = Arrays.asList(line.split(","));
                    Node n_server = new Node(parsed_server.get(0), parsed_server.get(1), parsed_server.get(2));
                    this.get_all_server_nodes().add(n_server);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.get_all_server_nodes().size());
            } finally {
                br.close();
            }
        } catch (Exception e) {
            System.out.println("setServerList exception: " + e);
        }
    }
    public void serverSocket(Integer server_id, Server current_server) {
        try {
            server = new ServerSocket(Integer.valueOf(this.all_server_nodes.get(server_id).port));
            id = Integer.toString(server_id);
            System.out.println("Server node running on port: " + Integer.valueOf(this.all_server_nodes.get(server_id).port));
            System.out.println("Use Ctrl + C to end");
            InetAddress host_server_ip = InetAddress.getLocalHost();
            String ip = host_server_ip.getHostAddress();
            String host_name = host_server_ip.getHostName();
            System.out.println("Current Server IP Address: " + ip);
            System.out.println("Current Server Hostname: " + host_name);
        } catch (IOException e) {
            System.out.println("serverSocket exception: Error creating socket: " + e);
            System.exit(-1);
        }
        Server.CommandParser cmdpsr = new Server.CommandParser(current_server);
        cmdpsr.start();
        Thread current_node = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Socket s = server.accept();
                        ServerSocketConnection serverSocketConnection = new ServerSocketConnection(s, id, false, current_server);
                        serverSocketConnectionList.add(serverSocketConnection);
                        serverSocketConnectionHashMap.put(serverSocketConnection.get_remote_id(), serverSocketConnection);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        current_node.setDaemon(true);
        current_node.start();
    }
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Server <server-number>");
            System.exit(1);
        }
        System.out.println("Starting the server");
        Server server = new Server();
        server.setServerList();
        server.setServerWorkFolder();
        server.serverSocket(Integer.valueOf(args[0]), server);
        System.out.println("Started Client with ID: " + server.get_id());
    }
}