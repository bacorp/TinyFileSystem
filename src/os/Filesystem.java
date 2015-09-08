package os;

/**
 * Framework code: no modification made.
 */
public interface Filesystem {

    /**
     * Create file with contents from array
     * @param contents
     * @return Index of file created
     */
    public int newFile(byte[] contents);
    
    /**
     * Create file with name and contents from array
     * @param filename
     * @param contents
     * @return Index of file created
     */
    public int newFile(String filename, byte[] contents);
    
    /**
     * Delete file at index
     * @param index
     */
    public void deleteFile(int index);
    
    /**
     * Delete file specified by full path/filename
     * @param filename
     */
    public void deleteFile(String filename);
    
    /**
     * Print all data stored on filesystem in readable format
     */
    public void dumpContents();
}
