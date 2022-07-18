public class Node {
    String id;
    String ipaddr;
    String port;
    public Node(String id, String ipaddr, String port) {
        this.id = id;
        this.ipaddr = ipaddr;
        this.port = port;
    }
    public String get_id() {
        return id;
    }
    public String get_ipaddr() {
        return ipaddr;
    }
    public String get_port() {
        return port;
    }
    public void set_id(String id) {
        this.id = id;
    }
    public void set_ipaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }
    public void set_port(String port) {
        this.port = port;
    }
}