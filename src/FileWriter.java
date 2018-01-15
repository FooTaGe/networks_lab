import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
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
    private long downloaded = 0;

    FileWriter(DownloadableMetadata downloadableMetadata, BlockingQueue<Chunk> chunkQueue) {
        this.chunkQueue = chunkQueue;
        this.downloadableMetadata = downloadableMetadata;
        fileSize = downloadableMetadata.getFilesize();
    }

    private void writeChunks() throws IOException {
        //TODO
        LinkedList<Chunk> tempList = new LinkedList<>();
        data = new RandomAccessFile(downloadableMetadata.getFilename(), "rws");
        metadataStream = new ObjectOutputStream(new FileOutputStream(downloadableMetadata.getMetadataFilename()));
        metadataBakStream = new ObjectOutputStream( new FileOutputStream(downloadableMetadata.getMetadataFilename() + ".bak"));
        boolean endMarkerNotSeen = true;
        while(endMarkerNotSeen){
            int numOfElements = chunkQueue.drainTo(tempList);
            endMarkerNotSeen = !checkIfDone(tempList, numOfElements);
            updateFile(tempList);
            updateMetadata(tempList);
            //TODO should this print be here?
            downloaded += (long)numOfElements * downloadableMetadata.getChunkSize();
            System.out.println("Downloaded: " + downloaded + " out of " + fileSize + "\n" + downloadableMetadata.getDone() + "%");
            //TODO
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e){

            }
            tempList.clear();
        }
        closeStreams();
    }

    private void closeStreams() throws IOException{
        data.close();
        metadataStream.close();
        metadataBakStream.close();
    }

    private void updateMetadata(LinkedList<Chunk> i_list) throws IOException{
        downloadableMetadata.addChunkList(i_list);
        try {
            //TODO
            safeWriteWithBackupMD5(downloadableMetadata);
            throw new NoSuchAlgorithmException();
        }
        catch (NoSuchAlgorithmException e){
            //Todo problem i need to print to screen and close program
        }
    }

    private void safeWriteWithBackupMD5(DownloadableMetadata downloadableMetadata) throws IOException, NoSuchAlgorithmException {
        metadataStream.writeObject(downloadableMetadata);
        metadataStream.flush();
        metadataBakStream.writeObject(downloadableMetadata);
        metadataBakStream.flush();
        //TODO look at https://stackoverflow.com/questions/415953/how-can-i-generate-an-md5-hash
    }


    private void updateFile(LinkedList<Chunk> i_list) throws IOException{
        for (Chunk i: i_list) {
            if(i.getData() != null) {
                if(i.getData().length != 4096){
                    System.out.println("Fuck");
                }
                data.seek(i.getOffset());
                data.write(i.getData(), 0, i.getSize_in_bytes());
            }
        }


    }



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
