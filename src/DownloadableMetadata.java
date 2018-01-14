import java.io.Serializable;
import java.util.List;

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
    private boolean[] m_chunkMap;
    private int m_chunkSize;
    private int m_point = 0;
    public final int PARTITION_SIZE = 100;



    DownloadableMetadata(String url, long i_fileSize, int i_chunkSize) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
        m_fileSize = i_fileSize;
        m_chunkSize = i_chunkSize;
        int arraySize = (int)(m_fileSize/i_chunkSize);
        m_chunkMap = new boolean[arraySize];
    }


    public static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    public static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    void addRange(Range range) {
        //TODO do the adding and stuff

    }

    void addChunkList(List<Chunk> i_chunkList){
        for (Chunk i: i_chunkList) {
            int position = chunkOffsetToPosition(i.getOffset());
            m_chunkMap[position] = true;
        }
    }

    private int chunkOffsetToPosition(long offset) {
        int last = offset % m_chunkSize == 0 ? 0 : 1;
        int position = (int)(offset / m_chunkSize) + last;
        return position;
    }

    String getFilename() {
        return filename;
    }

    String getMetadataFilename() {
        return metadataFilename;
    }

    boolean isCompleted() {
        for (boolean i: m_chunkMap) {
            if(i == false){
                return false;
            }
        }
        return true;
    }

    void delete() {
        //TODO
    }

    /**
     * use ResetPoint() to start over.
     * @return Range of the next missing range, returns null when reached end, does not mean all ranges arrived.
     */
    Range getMissingRange() {

        //return null if reached end of map
        if(m_point == m_chunkMap.length - 1){
            return null;
        }

        int start = m_point;
        start++;
        int end = start;
        // get start to the next empty space
        for (; start < m_chunkMap.length; start++) {
            if(m_chunkMap[start] == false){
                end = start;
                break;
            }
        }
        //get end to start + PARTION_SIZE or till end of space
        for (int i = 1; i < PARTITION_SIZE && i + start < m_chunkMap.length ; i++) {
            if(m_chunkMap[start + i] == true){
                break;
            }
            end++;
        }
        m_point = end;
        long rangeStart = positionToStartOffset(start);
        long rangeEnd = positionToEndOffset(end);
        return new Range(rangeStart, rangeEnd);
    }

    private long positionToEndOffset(int pos) {
        long ans = positionToStartOffset(pos);
        if(pos == m_chunkMap.length - 1){
            long remainder = m_fileSize % m_chunkSize;
            remainder = remainder == 0 ? m_chunkSize - 1 : remainder - 1;
            return ans + remainder;
        }

        return ans + m_chunkSize - 1;
    }

    void ResetPoint(){
        m_point = 0;
    }

    private long positionToStartOffset(int pos) {
        return (long)pos * m_chunkSize;
    }

    String getUrl() {
        return url;
    }

    public long getFilesize() {
        return m_fileSize;
    }

    public long getChunkSize() {
        return m_chunkSize;
    }
}
