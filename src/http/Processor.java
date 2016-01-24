package http;

import java.io.IOException;
import java.util.List;

public interface Processor {
    byte[] process(byte[] data, List<String> headers) throws IOException;
}