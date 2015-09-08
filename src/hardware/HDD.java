package hardware;

//import java.io.File;

/**
 * Provided for reference, the HDD class mimics operations available from a
 * basic disk controller.
 * 
 * Framework code: no modification made.
 */
public class HDD {

    //private static final File DISK_IMAGE = new File("default.bin");
    private byte[] data;
    private int size;
    private int index;

    /**
     * Create a HDD image from byte array
     * @param data 
     */
    public HDD(byte[] data) {
        this.data = data.clone();
        this.size = data.length;
        this.index = 0;
    }

    /**
     * Create a blank HDD image at given size
     * @param size in bytes
     */
    public HDD(int size) {
        this.data = new byte[size];
        this.size = size;
        this.index = 0;
    }

    /**
     * Write byte b to current position of read head, increment read head
     * afterward.
     * @param b
     */
    public void write(byte b) {
        data[index++] = b;
    }

    /**
     * Read value at current read head position, increment afterward.
     * @return
     */
    public byte read() {
        return data[index++];
    }

    /**
     * Distance between read head and end of disk - does NOT count unallocated
     * space!
     * @return
     */
    public int remaining() {
        return size - index;
    }

    /**
     * Reset read head to beginning of disk.
     */
    public void reset() {
        index = 0;
    }

    /**
     * Move read head to specified position
     * @param index
     */
    public void seek(int index) {
        this.index = index;
    }

    /**
     * Total size of disk
     * @return size in bytes
     */
    public int capacity() {
        return size;
    }
    
/*
 * The following requires Java 1.7 or above, and is not necessary, 
 * only convinient!
 */
    
//    /**
//     * Exports disk image to file - use to test your filesystem stores correctly
//     * @throws IOException
//     */
//    public void exprt() throws IOException {
//        Files.write(DISK_IMAGE.toPath(), data, StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING);
//    }
//
//    /**
//     * REPLACE current image with one loaded from file - use to test your
//     * filesystem works correctly!
//     * @throws IOException
//     */
//    public void imprt() throws IOException {
//        data = Files.readAllBytes(DISK_IMAGE.toPath());
//    }

}
