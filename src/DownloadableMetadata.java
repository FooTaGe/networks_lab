import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Describes a file's metadata: URL, file name, size, and which parts already downloaded to disk.
 *
 * The metadata (or at least which parts already downloaded to disk) is constantly stored safely in disk.
 * When constructing a new metadata object, we first check the disk to load existing metadata.
 *
 * CHALLENGE: try to avoid metadata disk footprint of O(n) in the average case
 * HINT: avoid the obvious bitmap solution, and think about ranges...
 */
public class DownloadableMetadata implements Serializable {
    private final String metadataFilename;
    private String filename;
    private String url;
    private long m_fileSize;
    private byte[] m_chunkMap;
    private int m_chunkSize;
    private int m_point = 0;
    public final int PARTITION_SIZE = 1000;
    Lock lock;
    private int lastDoneReturned = 0;
    private static final long serialVersionUID = 1337L;




    DownloadableMetadata(String url, long i_fileSize, int i_chunkSize) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        m_fileSize = i_fileSize;
        m_chunkSize = i_chunkSize;
        int arraySize = (int)(m_fileSize/i_chunkSize);
        arraySize += m_fileSize % i_chunkSize == 0 ? 0 : 1;
        m_chunkMap = new byte[arraySize];
        this.lock = new ReentrantLock(true);

    }

    /**
     * adds ".metadata" to the file name
     * @param filename
     * @return new name
     */
    public static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    /**
     * @param path
     * @return the name of the downloaded file

     */
    public static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    //unused
    void addRange(Range range) {

    }

    /**
     * updates chunkList
     * @param i_chunkList
     */
    void addChunkList(List<Chunk> i_chunkList){
        lock.lock();
        try {
            for (Chunk i : i_chunkList) {
                if (i.getData() != null) {
                    int position = chunkOffsetToPosition(i.getOffset());
                    m_chunkMap[position] = 1;
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * @param offset
     * @return chunk's offset position

     */
    private int chunkOffsetToPosition(long offset) {
        int position = (int)(offset / m_chunkSize);
        return position;
    }

    /**
     * @return filename
     */
    String getFilename() {
        return filename;
    }

    /**
     * @return metadata filename

     */
    String getMetadataFilename() {
        return metadataFilename;
    }

    /**
     * checks if done downloading
     * @return
     */
    boolean isCompleted() {
        lock.lock();
        try {
            for (byte i : m_chunkMap) {
                if (i == 0) {
                    return false;
                }
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    //unused
    void delete() {

    }

    /**
     * use ResetPoint() to start over.
     * @return Range of the next missing range, returns null when reached end, does not mean all ranges arrived.
     */
    Range getMissingRange() {
        lock.lock();
        try {
            //return null if reached end of map
            if (m_point >= m_chunkMap.length - 1) {
                return null;
            }

            int start = m_point;
            int end = start;
            // get start to the next empty space
            for (; start < m_chunkMap.length; start++) {
                if (m_chunkMap[start] == 0) {
                    end = start;
                    break;
                } else if (start == m_chunkMap.length - 1) {
                    m_point = m_chunkMap.length;
                    return null;
                }
            }
            //get end to start + PARTION_SIZE or till end of space
            for (int i = 1; i < PARTITION_SIZE && i + start < m_chunkMap.length; i++) {
                if (m_chunkMap[start + i] == 1) {
                    break;
                }
                end++;
            }
            m_point = end;
            long rangeStart = positionToStartOffset(start);
            long rangeEnd = positionToEndOffset(end);
            m_point++;
            return new Range(rangeStart, rangeEnd);
        } finally {
            lock.unlock();
        }
    }

    /**
     * translates position in chunk map to bytes offset in range
     * @param pos
     * @return start position in range
     */
    private long positionToEndOffset(int pos) {
        long ans = positionToStartOffset(pos);
        if(pos == m_chunkMap.length - 1){
            long remainder = m_fileSize % m_chunkSize;
            return ans + remainder - 1;
        }

        return ans + m_chunkSize - 1;

    }

    /**
     * resets pointer
     */
    void ResetPoint(){
        m_point = 0;
    }

    /**
     * moves pointer to the start of a chunk
     * @param pos
     * @return
     */
    private long positionToStartOffset(int pos) {
        return (long)pos * m_chunkSize;
    }

    //unused
    String getUrl() {
        return url;
    }

    /**
     * returns filesize
     */

    public long getFilesize() {
        return m_fileSize;
    }

    //unused
    public long getChunkSize() {
        return m_chunkSize;
    }

    /**
     * returns completed chunks
     * @return
     */
    public int getDone(){
        int count = 0;
        for (byte i: m_chunkMap) {
            count += i;
        }
        lastDoneReturned = (int)(count * 100 / (double)m_chunkMap.length);
        return lastDoneReturned;
    }

}
