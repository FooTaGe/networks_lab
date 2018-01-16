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

    /**
     * It reads CHUNK_SIZE at a time and writs it into a BlockingQueue.
     * It supports downloading a range of data, and limiting the download rate using a token bucket.
     * @throws IOException
     * @throws InterruptedException
     */
    private void downloadRange() throws IOException, InterruptedException {
        URL url = new URL(this.url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Range", GetRange());
        connection.connect();
        //TODO what todo if fail (not code 206)
        //System.out.println("Response Code: " + connection.getResponseCode());
        //System.out.println("Content-Length: " + connection.getContentLengthLong());

        InputStream inputStream = connection.getInputStream();
        long size = 0; //the amount of bytes downloaded by the thread
        int val; //the number of bytes read per iteration
        byte[] tempChunkData = new byte[CHUNK_SIZE];

        tokenBucket.take(CHUNK_SIZE); //take first CHUNK_SIZE tokens to initialize download

        while((val = readChunk(inputStream, tempChunkData)) != -1 ){
            Chunk chunk = new Chunk(tempChunkData, range.getStart() + size, val);
            size += val; //update size
            outQueue.add(chunk); //add new chunk of data to outQueue
            tokenBucket.take(CHUNK_SIZE); //take additional CHUNK_SIZE tokens to continue download
        }
        inputStream.close();
        connection.disconnect();
    }

    /**
     * reads an entire chunk from stream into data
     * @param stream
     * @param data
     * @return
     * @throws IOException
     */
    private int readChunk(InputStream stream, byte[] data) throws IOException {
        int count = 0;
        int val;
        for (; count < CHUNK_SIZE; count++) {
            val = stream.read();
            if (val == -1) {
                return count == 0 ? -1 : count;
            }
            data[count] = (byte) val;
        }

        return count;
    }

    public static int getChunkSize(){
        return CHUNK_SIZE;
    }


    public String GetRange(){
        return "bytes=" + range.getStart() + "-" + range.getEnd();
    }
    @Override

    public void run(){
        try {
            this.downloadRange();
        } catch (IOException | InterruptedException e) {
            if(e instanceof IOException){
                System.err.println("There was an error connecting to the server, please try again. Download failed");
            }
        }
    }
}
