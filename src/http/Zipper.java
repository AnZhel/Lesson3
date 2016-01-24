package http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by Andrey on 24.01.2016.
 */
public class Zipper implements Processor {
    private String boundary;
    public Zipper(String boundary){
        this.boundary = boundary;
    }

    @Override
    public byte[] process(byte[] data, List<String> headers) throws IOException{
        String contentString;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(os);
        zout.setLevel(Deflater.DEFAULT_COMPRESSION);
        contentString = new String(data,"CP1251");
        String contents[] = contentString.split("--"+boundary+"\r\n");
        HashMap<String,String> inputFiles  = new HashMap<>();
        for (String file:contents) {
            if (file.equals("")) continue;
            String fileName = file.substring(file.indexOf("filename=")+9,file.indexOf("\r\n"));
            fileName = fileName.replace("\"","");
            String fileContent = file.substring(file.indexOf("\r\n\r\n")+4);
            ZipEntry ze = new ZipEntry(fileName);
            zout.putNextEntry(ze);
            zout.write(fileContent.getBytes());
            zout.closeEntry();
        }
        zout.close();
        headers.add("Accept-Ranges: bytes\r\n");
        headers.add("Content-length: "+os.size()+"\r\n");
        headers.add("Content-type: application /zip\r\n");
        headers.add("Content-Disposition: attachment; filename = zipped.zip\r\n");
        headers.add("Connection: Keep-Alive\r\n\r\n");

        return os.toByteArray();
    }
}
