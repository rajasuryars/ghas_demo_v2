import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Client {
    String id;
    List<Node> all_client_nodes = new LinkedList<>();
    List<Node> all_server_nodes = new LinkedList<>();
    Integer logical_clock = 0;
    List<SocketConnection> socketConnectionList = new LinkedList<>();
    List<SocketConnection> socketConnectionListServer = new LinkedList<>();
    ServerSocket server;
    HashMap<String, SocketConnection> socketConnectionHashMap = new HashMap<>();
    HashMap<String, SocketConnection> socketConnectionHashMapServer = new HashMap<>();
    HashMap<String, Boolean> client_permission_required = new HashMap<>();
    Integer highest_logical_clock_value = 0;
    Integer outstanding_reply_count = 0;
    Boolean requested_cs = false;
    Boolean using_cs = false;
    List<String> deferred_reply_list = new LinkedList<>();
    String requested_cs_for_file;
    Integer minimum_delay = 5000;
    String available_file_list = "";
    Boolean critical_section_read_write_complete = true;
    String file_process_option = "RW";
    Integer server_count = 0;
    Integer write_ack_count = 0;
    public Client(String id) {
        this.id = id;
    }
    public String get_id() {
        return id;
    }
    public void set_id(String id) {
        this.id = id;
    }
    public List<Node> get_all_client_nodes() {
        return all_client_nodes;
    }
    public void set_all_client_nodes(List<Node> all_client_nodes) {
        this.all_client_nodes = all_client_nodes;
    }
    public List<Node> get_all_server_nodes() {
        return all_server_nodes;
    }
    public void set_all_server_nodes(List<Node> all_server_nodes) {
        this.all_server_nodes = all_server_nodes;
    }
    public Integer get_logical_clock() {
        return logical_clock;
    }
    public void set_logical_clock(Integer logical_clock) {
        this.logical_clock = logical_clock;
    }
    public class CommandParser extends Thread {
        Client current;
        public CommandParser(Client current) {
            this.current = current;
        }
        Pattern SETUP = Pattern.compile("^SETUP$");
        Pattern SERVER_SETUP = Pattern.compile("^SERVER_SETUP$");
        Pattern SERVER_SETUP_TEST = Pattern.compile("^SERVER_SETUP_TEST$");
        Pattern START = Pattern.compile("^START$");
        Pattern CONNECTION_DETAIL = Pattern.compile("^CONNECTION_DETAIL$");
        Pattern REQUEST = Pattern.compile("^REQUEST$");
        Pattern AUTO_REQUEST = Pattern.compile("^AUTO_REQUEST$");
        Pattern SHOW_FILES = Pattern.compile("^SHOW_FILES$");
        int rx_cmd(Scanner cmd) {
            String cmd_in = null;
            if (cmd.hasNext())
                cmd_in = cmd.nextLine();
                Matcher m_SETUP = SETUP.matcher(cmd_in);
                Matcher m_START = START.matcher(cmd_in);
                Matcher m_CONNECTION_DETAIL = CONNECTION_DETAIL.matcher(cmd_in);
                Matcher m_REQUEST = REQUEST.matcher(cmd_in);
                Matcher m_AUTO_REQUEST = AUTO_REQUEST.matcher(cmd_in);
                Matcher m_SERVER_SETUP = SERVER_SETUP.matcher(cmd_in);
                Matcher m_SERVER_SETUP_TEST = SERVER_SETUP_TEST.matcher(cmd_in);
                Matcher m_SHOW_FILES = SHOW_FILES.matcher(cmd_in);
                if (m_SETUP.find()) {
                    setupConnections(current);
                } else if (m_START.find()) {
                    System.out.println("Socket connection test function");
                    sendP();
                } else if (m_REQUEST.find()) {
                    System.out.println("Initiating request for file: A: critical section");
                    sendRequest("a");
                } else if (m_CONNECTION_DETAIL.find()) {
                    System.out.println("Number of socket connection");
                    System.out.println(socketConnectionList.size());
                    for (Integer i = 0; i < socketConnectionList.size(); i++) {
                        System.out.println("IP: " + socketConnectionList.get(i).get_other_client().getInetAddress() + " Port: " + socketConnectionList.get(i).get_other_client().getPort() + " ID: " + socketConnectionList.get(i).get_remote_id());
                    }
                    for (String key: socketConnectionHashMap.keySet()) {
                        System.out.println("Client ID: " + key + " Socket: " + socketConnectionHashMap.get(key).get_other_client().getPort());
                    }
                } else if (m_AUTO_REQUEST.find()) {
                    sendAutoRequest();
                } else if (m_SERVER_SETUP.find()) {
                    setupServerConnection(current);
                    enquireHostedFiles();
                } else if (m_SERVER_SETUP_TEST.find()) {
                    sendTestWrite();
                } else if (m_SHOW_FILES.find()) {
                    System.out.println("Hosted Files: " + available_file_list);
                }
            return 1;
        }
        public void run() {
            System.out.println("Enter START command to set-up MESH Connection: ");
            Scanner input = new Scanner(System.in);
            while (rx_cmd(input) != 0) {}
        }
    }
    public void sendTestWrite() {
        for (Integer remoteServer = 0; remoteServer < this.socketConnectionListServer.size(); remoteServer++) {
            socketConnectionListServer.get(remoteServer).serverWriteTest();
        }
    }
    public void setupConnections(Client current) {
        try {
            System.out.println("CONNECTING CLIENTS");
            for (Integer client_id = Integer.valueOf(this.id) + 1; client_id < all_client_nodes.size(); client_id++) {
                Socket client_connection = new Socket(this.all_client_nodes.get(client_id).get_ipaddr(), Integer.valueOf(all_client_nodes.get(client_id).get_port()));
                SocketConnection socket_connection = new SocketConnection(client_connection, this.get_id(), true, current);
                if (socket_connection.get_remote_id() == null) { 
                    socket_connection.set_remote_id(Integer.toString(client_id));
                }
                socketConnectionList.add(socket_connection);
                socketConnectionHashMap.put(socket_connection.get_remote_id(), socket_connection);
                client_permission_required.put(socket_connection.get_remote_id(), true);
            }
        } catch (Exception e) {
            System.out.println("setupConnections exception: " + e);
        }
    }
    public void setupServerConnection(Client current) {
        try {
            System.out.println("CONNECTING SERVER");
            for (Integer server_id = 0; server_id < all_server_nodes.size(); server_id++) {
                Socket serverConnection = new Socket(this.all_server_nodes.get(server_id).get_ipaddr(), Integer.valueOf(this.all_server_nodes.get(server_id).get_port()));
                SocketConnection socketConnectionServer = new SocketConnection(serverConnection, this.get_id(), true, current);
                if (socketConnectionServer.get_remote_id() == null) {
                    socketConnectionServer.set_remote_id(Integer.toString(server_id));
                }
                socketConnectionListServer.add(socketConnectionServer);
                socketConnectionHashMapServer.put(socketConnectionServer.get_remote_id(), socketConnectionServer);
            }
            this.server_count = socketConnectionListServer.size();
        } catch (Exception e) {
            System.out.println("setupServerConnection exception: " + e);
        }
    }
    public void sendP() {
        System.out.println("Sending P");
        for (Integer i = 0; i < this.socketConnectionList.size(); i++) {
            socketConnectionList.get(i).publish();
        }
    }
    public synchronized void fileReadAckProcessor(String resp_server_id, String file_name, Message message) {
        System.out.println("Processing read from file request acknowledge");
        System.out.println("CRITICAL SECTION READ - COMPLETED");
        System.out.println("LAST MESSAGE ON FILE: " + file_name + " HAD CLIENT ID: " + message.get_client_id() + " AND TIMESTAMP: " + message.get_timestamp());
        this.critical_section_read_write_complete = true;
        release_cs_cleanup();
    }
    public synchronized void processWriteAck(String file_name) {
        System.out.println("Inside WRITE_TO_FILE_ACK processor");
        if (file_name.equals(this.requested_cs_for_file)) {
            this.write_ack_count--;
            System.out.println(this.write_ack_count);
            if (this.write_ack_count == 0) {
                this.critical_section_read_write_complete = true;
                System.out.println("WRITE TO FILE COMPLETE");
                release_cs_cleanup();
            }
        }
    }
    public void sendAutoRequest() {
        Thread sendAuto = new Thread() {
            public void run() {
                try {
                    System.out.println("Auto - Generating Request");
                    while (true) {
                        // System.out.println("Auto - Generating Request");
                        if (available_file_list.length() != 0) {
                            Random r = new Random();
                            char file = available_file_list.charAt(r.nextInt(available_file_list.length()));
                            String file_name = file + ".txt";
                            sendRequest(file_name);
                            double randFraction = Math.random() * 1000;
                            Integer delay = (int)Math.floor(randFraction) + minimum_delay; 
                            System.out.println("THE AUTO REQUEST thread will sleep for " + delay + " seconds");
                            Thread.sleep(delay);
                        }
                    }
                } catch (Exception e) {
                    System.out.println("sendAutoRequest exception: " + e);
                }
            }
        };
        sendAuto.setDaemon(true);
        sendAuto.start();
    }
    public synchronized void processRequest(String req_client_id, Integer req_client_logical_clock, String file_name) {
        if (file_name.equals(this.requested_cs_for_file)) {
            System.out.println("Inside Process Request for request client: " + req_client_id + " which had logical clock value of: " + req_client_logical_clock);
            this.highest_logical_clock_value = Math.max(this.highest_logical_clock_value, req_client_logical_clock);
            if (this.using_cs || this.requested_cs) {
                if (req_client_logical_clock > this.logical_clock) {
                    System.out.println("USING OR REQUESTED CS");
                    System.out.println("Highest Logical Clock Value: " + this.highest_logical_clock_value);
                    System.out.println("Current Logical Clock Value: " + this.logical_clock);
                    System.out.println("*************** SHOULD DEFER *************** CONDITION 1 ***************");
                } else if (req_client_logical_clock == this.logical_clock) {
                    System.out.println("USING OR REQUESTED CS");
                    System.out.println("Highest Logical Clock Value: " + this.highest_logical_clock_value);
                    System.out.println("Current Logical Clock Value: " + this.logical_clock);
                    System.out.println("*************** SHOULD DEFER *************** CONDITION 2 ***************");
                }
            }
            if (((this.using_cs || this.requested_cs) && (req_client_logical_clock > this.logical_clock)) || ((this.using_cs || this.requested_cs) && req_client_logical_clock == this.logical_clock && Integer.valueOf(req_client_id) > Integer.valueOf(this.get_id()))) {
                System.out.println("------------------------------------------------------------------------");
                System.out.println("Deferred Reply for Request Client: " + req_client_id + " which had logical clock value of: " + req_client_logical_clock);
                System.out.println("Critical Section Access from this node had Client ID: " + this.get_id() + " and last updated logical clock is: " + this.logical_clock);
                System.out.println("------------------------------------------------------------------------");
                this.client_permission_required.replace(req_client_id, true);
                this.deferred_reply_list.add(req_client_id);
            } else {
                System.out.println("Initiating SEND REPLY without block as defer condition is not met for the same file " + this.requested_cs_for_file + file_name);
                this.client_permission_required.replace(req_client_id, true);
                SocketConnection requestingSocketConnection = socketConnectionHashMap.get(req_client_id);
                requestingSocketConnection.reply(file_name);
            }
        } else {
            System.out.println("Inside Process Request for ** DIFFERENT FILE ** request Client: " + req_client_id + " which had logical clock value of: " + req_client_logical_clock);
            this.highest_logical_clock_value = Math.max(this.highest_logical_clock_value, req_client_logical_clock);
            System.out.println("Initiating SEND REPLY without block");
            this.client_permission_required.replace(req_client_id, true);
            SocketConnection requestingSocketConnection = socketConnectionHashMap.get(req_client_id);
            requestingSocketConnection.reply(file_name);
        }
    }
    public synchronized void processReply(String rep_client_id, String file_name) {
        if (file_name.equals(this.requested_cs_for_file)) {
            System.out.println("Inside Process Reply for replying client: " + rep_client_id + " for the file " + file_name);
            this.client_permission_required.replace(rep_client_id, false);
            this.outstanding_reply_count--;
            if (this.outstanding_reply_count == 0) {
                enterCriticalSection(file_name);
            }
        } else {
            System.out.println("Inside Process Reply for replying Client: " + rep_client_id + " for the file " + file_name + " ### NO ACTION TAKEN");
        }
    }
    public synchronized void sendRequest(String file_name) {
        if (!(this.requested_cs || this.using_cs)) {
            this.requested_cs = true;
            this.requested_cs_for_file = file_name;
            this.logical_clock = this.highest_logical_clock_value + 1;
            System.out.println("Sending Request with logical clock: " + this.logical_clock + " requesting CS access for file " + this.requested_cs_for_file);
            for (Integer i = 0; i < this.socketConnectionList.size(); i++) {
                if (client_permission_required.get(socketConnectionList.get(i).get_remote_id()) == true) {
                    this.outstanding_reply_count++;
                    socketConnectionList.get(i).request(this.logical_clock, this.requested_cs_for_file);
                }
            }
            if (this.outstanding_reply_count == 0) {
                enterCriticalSection(file_name);
            }
        } else {
            System.out.println("Currently in CS or already requested for CS");
        }
    }
    public void enterCriticalSection(String file_name) {
        System.out.println("Entering critical section READ/WRITE TO SERVER");
        this.using_cs = true;
        this.requested_cs = false;
        this.critical_section_read_write_complete = false;
        Random r = new Random();
        char readOrWrite = file_process_option.charAt(r.nextInt(file_process_option.length()));
        try {
            System.out.println("====================== ENTERING CRITICAL SECTION =======================");
            if (readOrWrite == 'R') {
                System.out.println("CRITICAL SECTION READ OPTION");
                Integer server_number = r.nextInt(this.get_all_server_nodes().size());
                String server_id = Integer.toString(server_number);
                this.critical_section_read_write_complete = false;
                socketConnectionHashMapServer.get(server_id).read(file_name);
                System.out.println("SERVER: " + server_id + " FILE: " + file_name + " PROCESS OPTION: READ");
            } else if (readOrWrite == 'W') {
                System.out.println("CRITICAL SECTION WRITE OPTION");
                this.write_ack_count = this.server_count;
                for (Integer serverConnectIndex = 0; serverConnectIndex < this.socketConnectionListServer.size(); serverConnectIndex++) {
                    this.socketConnectionListServer.get(serverConnectIndex).write(file_name, new Message(this.get_id(), Integer.toString(this.logical_clock)));
                }
                System.out.println("SERVER: ALL FILE: " + file_name + " PROCESS OPTION: WRITE");
            }
            System.out.println("====================== EXITING CRITICAL SECTION ========================");
        } catch (Exception e) {
            System.out.println("enterCriticalSection exception: " + e);
        }
    }
    public void release_cs_cleanup() {
        System.out.println("Received necessary acknowledgement");
        System.out.println("========= ENTERING CLEAN UP: SEND DEFERRED REPLY AND FLAG RESET ========");
        this.using_cs = false;
        this.requested_cs = false;
        Iterator<String> deferred_reply_client_id = deferred_reply_list.iterator();
        while (deferred_reply_client_id.hasNext()) {
            socketConnectionHashMap.get(deferred_reply_client_id.next()).reply(this.requested_cs_for_file);
        }
        this.requested_cs_for_file = "";
        deferred_reply_list.clear();
        System.out.println("=========================== EXITING CLEAN UP ===========================");
    }
    public void clientSocket(Integer client_id, Client current) {
        try {
            server = new ServerSocket(Integer.valueOf(this.all_client_nodes.get(client_id).port));
            id = Integer.toString(client_id);
            System.out.println("Client node running on port: " + Integer.valueOf(this.all_client_nodes.get(client_id).port));
            System.out.println("Use Ctrl + C to end");
            InetAddress host_ip = InetAddress.getLocalHost();
            String ipaddr = host_ip.getHostAddress();
            String host_name = host_ip.getHostName();
            System.out.println("Current IP Address: " + ipaddr);
            System.out.println("Current Hostname:" + host_name);
        } catch (IOException e) {
            System.out.println("Error creating socket");
            System.exit(-1);
        }
        CommandParser cmdpsr = new CommandParser(current);
        cmdpsr.start();
        Thread current_node = new Thread() {
            public void run() {
                while (true) {
                    try {
                        Socket s = server.accept();
                        SocketConnection socketConnection = new SocketConnection(s, id, false, current);
                        socketConnectionList.add(socketConnection);
                        socketConnectionHashMap.put(socketConnection.get_remote_id(), socketConnection);
                        client_permission_required.put(socketConnection.get_remote_id(), true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        current_node.setDaemon(true);
        current_node.start();
    }
    public synchronized void setHostedFiles(String hosted_files) {
        this.available_file_list = hosted_files;
    }
    public void enquireHostedFiles() {
        socketConnectionHashMapServer.get("0").sendEnquire();
    }
    public void setClientList() {
        try {
            BufferedReader br = new BufferedReader(new FileReader("ClientAddressAndPorts.txt"));
            try {
                StringBuilder sb = new StringBuilder();
                String line = br.readLine();
                while (line != null) {
                    sb.append(line);
                    List<String> parsed_client = Arrays.asList(line.split(","));
                    Node n_client = new Node(parsed_client.get(0), parsed_client.get(1), parsed_client.get(2));
                    this.get_all_client_nodes().add(n_client);
                    sb.append(System.lineSeparator());
                    line = br.readLine();
                }
                String everything = sb.toString();
                System.out.println(everything);
                System.out.println(this.get_all_client_nodes().size());
            } finally {
                br.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java Client <client-number>");
            System.exit(1);
        }
        System.out.println("Starting the Client");
        Client C1 = new Client(args[0]);
        C1.setClientList();
        C1.setServerList();
        C1.clientSocket(Integer.valueOf(args[0]), C1);
        System.out.println("Started Client with ID: " + C1.get_id());
    }
}