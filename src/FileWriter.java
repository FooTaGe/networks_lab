import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 * NOTE: make sure that the file interface you choose writes every update to the file's content or metadata
 *       synchronously to the underlying storage device.
 */
public class FileWriter implements Runnable {

    private final BlockingQueue<Chunk> chunkQueue;
    private DownloadableMetadata downloadableMetadata;
    private RandomAccessFile data;
    private ObjectOutputStream metadataStream;
    private ObjectOutputStream metadataBakStream;
    private long fileSize;
    private int downloaded = -1;

    FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.downloadableMetadata = downloadableMetadata;
        fileSize = downloadableMetadata.getFilesize();
    }

    /**
     * gather chunks and write into file
     * @throws IOException
     */
    private void writeChunks() throws IOException {
        LinkedList<Chunk> tempList = new LinkedList<>();
        data = new RandomAccessFile(downloadableMetadata.getFilename(), "rws");
        metadataStream = new ObjectOutputStream(new FileOutputStream(downloadableMetadata.getMetadataFilename()));
        metadataBakStream = new ObjectOutputStream( new FileOutputStream(downloadableMetadata.getMetadataFilename() + ".bak"));

        boolean endMarkerNotSeen = true;
        while(endMarkerNotSeen){
            try{
                Thread.sleep(500);
            }
            catch (InterruptedException e){

            }
            int numOfElements = chunkQueue.drainTo(tempList);
            endMarkerNotSeen = !checkIfDone(tempList, numOfElements);
            updateFile(tempList);
            updateMetadata(tempList);
            int tempDone;
            if((tempDone = downloadableMetadata.getDone()) != downloaded) {
                downloaded = tempDone;
                System.out.println("Downloaded: " + downloaded + "%");
            }
            tempList.clear();
        }
        closeStreams();
    }

    /**
     * closes streams
     * @throws IOException
     */
    private void closeStreams() throws IOException{
        data.close();
        metadataStream.close();
        metadataBakStream.close();
    }

    /**
     * updates metadata
     * @param i_list
     * @throws IOException
     */
    private void updateMetadata(LinkedList<Chunk> i_list) throws IOException{
        downloadableMetadata.addChunkList(i_list);
        safeWriteWithBackup(downloadableMetadata);
    }

    /**
     * Writes the metadata in a safe way
     * @param downloadableMetadata
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    private void safeWriteWithBackup(DownloadableMetadata downloadableMetadata) throws IOException {
        try (ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(downloadableMetadata.getMetadataFilename()))) {
            writer.writeObject(downloadableMetadata);
            writer.flush();
            writer.close();
        }
        try (ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(downloadableMetadata.getMetadataFilename() + ".bak"))) {
            writer.writeObject(downloadableMetadata);
            writer.flush();
            writer.close();
        }
    }

    /**
     * updates file
     * @param i_list
     * @throws IOException
     */
    private void updateFile(LinkedList<Chunk> i_list) throws IOException{
        for (Chunk i: i_list) {
            if(i.getData() != null) {
                data.seek(i.getOffset());
                data.write(i.getData(), 0, i.getSize_in_bytes());
            }
        }
    }

    /**
     * checks if done
     * @param i_list
     * @param i_numOfElements
     * @return
     */
    private boolean checkIfDone(LinkedList<Chunk> i_list, int i_numOfElements) {
        if(i_numOfElements > 0){
            Chunk lastChunk = i_list.getLast();
            return  lastChunk.getData() == null;
        }
        else return false;
    }

    @Override
    public void run() {
        try {
            this.writeChunks();
        } catch (IOException e) {
            e.printStackTrace();
            //TODO
        }
    }
}
