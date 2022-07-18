public class WriteRequest {
    String file_name;
    Message message;
    public WriteRequest(String file_name, Message message) {
        this.file_name = file_name;
        this.message = message;
    }
    public String get_file_name() {
        return file_name;
    }
    public Message get_message() {
        return message;
    }
    public void set_file_name(String file_name) {
        this.file_name = file_name;
    }
    public void set_message(Message message) {
        this.message = message;
    }
}