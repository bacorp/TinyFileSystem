package os;

/**
 * this class represents an inode on disk, each for a file
 *
 */
public class MetaData {
	
	private int index;
	private String fileName;
	private int[] blocks;
	private int location; // location on disk in bytes
	
	
	public int getIndex() {
		return index;
	}
	public void setIndex(int index) {
		this.index = index;
	}
	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public int[] getBlocks() {
		return blocks;
	}
	public void setBlocks(int[] blocks) {
		this.blocks = blocks;
	}
	public int getLocation() {
		return location;
	}
	public void setLocation(int location) {
		this.location = location;
	}

	
	
}
