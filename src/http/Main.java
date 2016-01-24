package http;

import java.lang.Thread;

public class Main {
    public static void main(String[] args) {
        final HTTPServer server = new HTTPServer(11010, "C:\\temp");
        server.start();

        System.out.println("Server started...");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                server.stop();
                System.out.println("Server stopped!");
            }
        });
    }
}
