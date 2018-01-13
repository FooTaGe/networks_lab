import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class IdcDm {

    static long filesize;

    /**
     * Receive arguments from the command-line, provide some feedback and start the download.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        int numberOfWorkers = 1;
        Long maxBytesPerSecond = null;

        if (args.length < 1 || args.length > 3) {
            System.err.printf("usage:\n\tjava IdcDm URL [MAX-CONCURRENT-CONNECTIONS] [MAX-DOWNLOAD-LIMIT]\n");
            System.exit(1);
        } else if (args.length >= 2) {
            numberOfWorkers = Integer.parseInt(args[1]);
            if (args.length == 3)
                maxBytesPerSecond = Long.parseLong(args[2]);
        }

        String url = args[0];

        System.err.printf("Downloading");
        if (numberOfWorkers > 1)
            System.err.printf(" using %d connections", numberOfWorkers);
        if (maxBytesPerSecond != null)
            System.err.printf(" limited to %d Bps", maxBytesPerSecond);
        System.err.printf("...\n");

        DownloadURL(url, numberOfWorkers, maxBytesPerSecond);
    }

    /**
     * Initiate the file's metadata, and iterate over missing ranges. For each:
     * 1. Setup the Queue, TokenBucket, DownloadableMetadata, FileWriter, RateLimiter, and a pool of HTTPRangeGetters
     * 2. Join the HTTPRangeGetters, send finish marker to the Queue and terminate the TokenBucket
     * 3. Join the FileWriter and RateLimiter
     *
     * Finally, print "Download succeeded/failed" and delete the metadata as needed.
     *
     * @param url URL to download
     * @param numberOfWorkers number of concurrent connections
     * @param maxBytesPerSecond limit on download bytes-per-second
     */
    private static void DownloadURL(String url, int numberOfWorkers, Long maxBytesPerSecond) {
        TokenBucket tokenBucket;
        RateLimiter rateLimiter;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfWorkers);
        DownloadableMetadata metadata;

        //init blockingQueue
        BlockingQueue<Chunk> queue = new LinkedBlockingQueue<>();

        //init tokenBucket
        tokenBucket = initTokenBucket(maxBytesPerSecond);

        //init metadata
        metadata = initMetaData(url);

        //init rateLimiter
        rateLimiter = new RateLimiter(tokenBucket, maxBytesPerSecond);
        //execute ratelimiter
        new Thread(rateLimiter).start();


        //todo is this the right place for this?
        //init fileWriter
        FileWriter fileWriter = new FileWriter(metadata, queue);
        //execute
        new Thread(fileWriter).start();

        /**
        //init executer ranges
        long startRange = 0;
        long filesize = 0;
        //TODO decie how to deal with this exeption
        try {
            filesize = getContentLength(url);
        }
        catch (IOException e){
            //TODO do something- cannot continue without files size unless using 1 thread only
        }
        long rangeSize = filesize / numberOfWorkers;
       for (int i = 0; i < numberOfWorkers; i++){
           Range thisRange = i == numberOfWorkers - 1 ? new Range(startRange, filesize - 1) :
                   new Range(startRange, startRange + rangeSize - 1);
           executor.execute(new HTTPRangeGetter(url, thisRange, queue, tokenBucket));
           startRange += rangeSize;
       }
        **/

        while(metadata.isCompleted()){
            Range nextRange;
            while((nextRange = metadata.getMissingRange()) != null){
                executor.execute(new HTTPRangeGetter(url, nextRange, queue, tokenBucket));
            }
            metadata.ResetPoint();
        }



        //TODO make this proper
        //todo when finished all ranges
        executor.shutdown();
        tokenBucket.terminate();
        queue.add(new Chunk(null, 0,0));



        //TODO
    }

    private static long getContentLength(String i_url) throws IOException{
        URL url = new URL(i_url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.connect();
        long contentLength = connection.getContentLength();
        connection.disconnect();
        return contentLength;
    }

    private static DownloadableMetadata initMetaData(String url) {
        String metadataName = DownloadableMetadata.getMetadataName(DownloadableMetadata.getName(url));
        DownloadableMetadata metadata = null;
        if(Files.exists(Paths.get(metadataName))){
             if(!tryLoadMetadata(metadataName, metadata)){
                 if(!tryLoadMetadata(metadataName + ".bak", metadata)){
                     metadata = new DownloadableMetadata(url, filesize, HTTPRangeGetter.getChunkSize());
                 }
             }
             return metadata;
        }
        else{
            return new DownloadableMetadata(url, filesize, HTTPRangeGetter.getChunkSize());
        }
    }

    private static boolean tryLoadMetadata(String metadataName, DownloadableMetadata metadata) {
        //todo try to read and deserialize, then check if is the samse as MD5
        //todo update metadata in case of success
        return false;
    }

    private static TokenBucket initTokenBucket(Long i_maxBytesPerSecond) {
        TokenBucket tokenBucket;
        if (i_maxBytesPerSecond == null) {
            tokenBucket = new TokenBucket(Long.MAX_VALUE);
        } else {
            tokenBucket = new TokenBucket(i_maxBytesPerSecond * 10);
        }
        return tokenBucket;
    }
}
