package http;

import java.lang.Exception;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.lang.Thread;

public class Client implements Runnable {
    private Socket socket;
    private FileManager fm;
    
    public Client(Socket socket, String path) {
        this.socket = socket;
        fm = new FileManager(path);
    }

    private void returnStatusCode(int code, OutputStream os) throws IOException {
        String msg = null;

        switch (code) {
            case 400:
                msg = "HTTP/1.1 400 Bad Request";
                break;
            case 404:
                msg = "HTTP/1.1 404 Not Found";
                break;
            case 500:
                msg = "HTTP/1.1 500 Internal Server Error";
                break;
        }

        byte[] resp = msg.concat("\r\n\r\n").getBytes();
        os.write(resp);
    }
    
    private byte[] getBinaryHeaders(List<String> headers) {
        StringBuilder res = new StringBuilder();

        for (String s : headers) 
            res.append(s);
            
        return res.toString().getBytes();
    }
    
    private void process(String request, OutputStream os) throws IOException {
        System.out.println(request);
        System.out.println("---------------------------------------------");

        int idx = request.indexOf("\r\n");
        request = request.substring(0, idx);

        String[] parts = request.split(" ");
        if (parts.length != 3) {
            returnStatusCode(400, os);
            return;
        }

        String method = parts[0], url = parts[1], version = parts[2];
        
        if (( ! version.equalsIgnoreCase("HTTP/1.0")) && ( ! version.equalsIgnoreCase("HTTP/1.1"))) {
            returnStatusCode(400, os);
            return;
        }
        if ( ! method.equalsIgnoreCase("GET")) {
            returnStatusCode(400, os);
            return;
        }
        if ("/".equals(url))
            url = "/index.html";
        
        List<String> headers = new ArrayList<String>();
        headers.add("HTTP/1.1 200 OK\r\n");
        
        byte[] content = fm.get(url);
        if (content == null) {
            returnStatusCode(404, os);
            return;
        }
        
        ProcessorsList pl = new ProcessorsList();
        pl.add(new Compressor(6));
        pl.add(new Chunker(30)); // comment
        content = pl.process(content, headers);

        if (content == null) {
            returnStatusCode(500, os);
            return;
        }

        // uncomment next line
        // headers.add("Content-Length: " + content.length + "\r\n");
        headers.add("Connection: close\r\n\r\n");
            
        os.write(getBinaryHeaders(headers));
        os.write(content);
    }

    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();

            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            byte[] buf, temp;
            int len, b;

            try {
                do {
                    len = is.available();
                    buf = new byte[len];

                    if (is.read(buf) > 0)
                        bs.write(buf);

                    temp = bs.toByteArray();

                    if (temp.length > 4 && temp[temp.length-4]==(byte)13 && temp[temp.length-3]==(byte)10 &&
                            temp[temp.length-2]==(byte)13 && temp[temp.length-1]==(byte)10){
                        String request = new String(temp, 0, temp.length-4);
                        process(request, os);

                    }


                    /* optimised
                    for (int i = 0; i < temp.length - 3; i++) {
                        if ((temp[i] == (byte)13) && (temp[i + 1] == (byte)10) &&
                            (temp[i + 2] == (byte)13) && (temp[i + 3] == (byte)10))
                        {
                            String request = new String(temp, 0, i);
                            process(request, os);
                        }
                    }*/
                } while ( ! Thread.currentThread().isInterrupted());
            } finally {
                socket.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}