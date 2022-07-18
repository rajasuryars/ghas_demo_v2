public class Message {
    String client_id;
    String timestamp;
    public Message(String client_id, String timestamp) {
        this.client_id = client_id;
        this.timestamp = timestamp;
    }
    public String get_client_id() {
        return client_id;
    }
    public String get_timestamp() {
        return timestamp;
    }
    public void set_client_id(String client_id) {
        this.client_id = client_id;
    }
    public void set_timestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}