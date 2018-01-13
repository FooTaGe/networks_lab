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
public class DownloadableMetadata {
    private final String metadataFilename;
    private String filename;
    private String url;

    private List<Range> ranges;


    DownloadableMetadata(String url) {
        this.url = url;
        this.filename = getName(url);
        this.metadataFilename = getMetadataName(filename);
    }


    private static String getMetadataName(String filename) {
        return filename + ".metadata";
    }

    private static String getName(String path) {
        return path.substring(path.lastIndexOf('/') + 1, path.length());
    }

    void addRange(Range range) {
        //TODO do the adding and stuff

    }

    void addChunkList(List<Chunk> i_chunkList){
        //TODO
    }

    String getFilename() {
        return filename;
    }

    String getMetadataFilename() {
        return metadataFilename;
    }

    boolean isCompleted() {
        //TODO
        return false;

    }

    void delete() {
        //TODO
    }

    Range getMissingRange() {
        //TODO
        return null;
    }

    String getUrl() {
        return url;
    }
}
