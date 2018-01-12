import com.sun.javafx.binding.StringFormatter;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.BlockingQueue;
import java.net.*;

/**
 * A runnable class which downloads a given url.
 * It reads CHUNK_SIZE at a time and writs it into a BlockingQueue.
 * It supports downloading a range of data, and limiting the download rate using a token bucket.
 */
public class HTTPRangeGetter implements Runnable {
    static final int CHUNK_SIZE = 4096;
    private static final int CONNECT_TIMEOUT = 500;
    private static final int READ_TIMEOUT = 2000;
    private final String url;
    private final Range range;
    private final BlockingQueue<Chunk> outQueue;
    private TokenBucket tokenBucket;

    HTTPRangeGetter(String url, Range range, BlockingQueue<Chunk> outQueue, TokenBucket tokenBucket) {
        this.url = url;
        this.range = range;
        this.outQueue = outQueue;
        this.tokenBucket = tokenBucket;
    }

    private void downloadRange() throws IOException, InterruptedException {
        //TODO
        //Todo ask to download range. each CHUNK_SIZE create a chunk and add to outQueue
        //TODO Throw if failed
        HttpURLConnection  connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("Range", GetRange());
        connection.connect();

        System.out.println("Respnse Code: " + connection.getResponseCode());
        System.out.println("Content-Length: " + connection.getContentLengthLong());
        InputStream inputStream = connection.getInputStream();
        int size = 0;
        int val;
        byte[] tempChunkData = new byte[4096];
        while((val = inputStream.read()) != -1 ){
            size++;
            if(size > CHUNK_SIZE){
                //TODO add chunk to outQueue and create new chunk. Set offset of each chunk to it true position meaning range + position.
                size = 0;
            }
            tempChunkData[size] = (byte)val;

        }
            //TODO create last chunk with remaining bytes.


    }

    public String GetRange(){
        return "bytes=" + range.getStart() + "-" + range.getEnd();
    }
    @Override
    public void run() {
        try {
            this.downloadRange();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            //TODO
        }
    }
}
