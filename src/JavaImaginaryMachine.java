
import hardware.HDD;
import os.Filesystem;
import os.NullFS;

/**
 *
 * @author t.harvey@sussex.ac.uk
 */
public class JavaImaginaryMachine {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JavaImaginaryMachine jim = new JavaImaginaryMachine();
    }

    /**
     * Initialise a disk image with default parameters and test.
     */
    public JavaImaginaryMachine() {
        int diskSize = 1024 * 1024;
        int blockSize = 64;
        int rootDirSize = 32; // maximum number of file
        HDD hdd = new HDD(diskSize);

        exampleTest(hdd, blockSize, rootDirSize);

    }

    /**
     * Simple example for evaluating file fragmentation on disk
     * @param hdd
     * @param blockSize
     * @param rootDirSize 
     */
    private void exampleTest(HDD hdd, int blockSize, int rootDirSize) {
        Filesystem fs = NullFS.format(hdd, blockSize, rootDirSize);
        
        int one = fs.newFile("hello", "1tricky tricky tricky".getBytes());
        int two = fs.newFile("hello3", "2tricky tricky tricky ".getBytes());
        
        int three = fs.newFile("hello2", "3tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky".getBytes());
        
        int four = fs.newFile("hello2", "4tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky tricky".getBytes());
        fs.deleteFile("hello2");
        
        //fs.dumpContents();
        int addrLong = fs.newFile("isk longest file you'll see on disk longest file you'll see on disk".getBytes());
        fs.dumpContents();
        System.out.println();
    }
    
}
