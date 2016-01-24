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

    private void getMethod(String[] requests, OutputStream os, List<String> headers) throws IOException {
        String[]  parts = requests[0].split(" ");
        String url = parts[1];
        if ("/".equals(url))
        url = "/index.html";
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
        headers.add("Content-Length: " + content.length + "\r\n");
        headers.add("Connection: close\r\n\r\n");

        os.write(getBinaryHeaders(headers));
        os.write(content);
    }

    private void postMethod(String[] requests, OutputStream os, InputStream is, List<String> headers) throws  IOException{
        String[] contentLength = requests[3].split(": ");
        int length = Integer.parseInt(contentLength[1]);
        String boundary = requests[9].substring(requests[9].indexOf("boundary="));
        boundary = boundary.replace("boundary=","");
        byte[] content = new byte[length];
        for (int i = 0; i < length; i++) content[i]=(byte)is.read();
        ProcessorsList pl = new ProcessorsList();
        pl.add(new Zipper(boundary));
        content = pl.process(content, headers);
        if (content==null){
            returnStatusCode(500, os);
            return;
        }
        os.write(getBinaryHeaders(headers));
        os.write(content);
    }

    private void process(String request, OutputStream os, InputStream is) throws IOException {
        System.out.println(request);
        System.out.println("---------------------------------------------");
        String[] requests = request.split("\r\n");
        List<String> headers;
        if (requests.length==0){
            returnStatusCode(400, os);
            return;
        }

        String[] parts = requests[0].split(" ");
        if (parts.length != 3) {
            returnStatusCode(400, os);
            return;
        }

        String method = parts[0], version = parts[2];
        
        if (( ! version.equalsIgnoreCase("HTTP/1.0")) && ( ! version.equalsIgnoreCase("HTTP/1.1"))) {
            returnStatusCode(400, os);
            return;
        }
        if ((! method.equalsIgnoreCase("GET"))&&(! method.equalsIgnoreCase("POST"))) {
            returnStatusCode(400, os);
            return;
        }
        headers = new ArrayList<String>();
        headers.add("HTTP/1.1 200 OK\r\n");

        if (method.equalsIgnoreCase("GET")){
            getMethod(requests,os,headers);
        }

        if (method.equalsIgnoreCase("Post")){
            postMethod(requests,os,is,headers);
        }
    }

    public void run() {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            try {
                do {
                    String request = "";
                    String eof = "";
                    int read;
                    while (!(eof.equals("\r\n\r\n")) && (read = is.read()) != -1) {
                        request += (char) read;
                        if (request.length() >= 4)
                            eof = request.substring(request.length() - 4);
                    }
                    process(request, os, is);
                } while (! Thread.currentThread().isInterrupted());
            }
            finally {socket.close();}
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }
    }
}