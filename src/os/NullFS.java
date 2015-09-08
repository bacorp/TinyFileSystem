package os;

import hardware.HDD;

/**
 * Note:
 * 
 * 1. This file system uses i-node for storing meta-data of each file.
 * 2. Meta-data in i-node includes: the file name, size and pointers to file data.
 * 3. File data is divided into several blocks, saved in uncontiguous style on disk.
 * 4. Deleting file will result empty block on disk, and can be used by other new created file.
 * 
 * Disk: |- block array size 4 bytes -|- block array indicate free one # bytes -|- total number of blocks 4 bytes -|- number of blocks used 4 bytes -|- number of files 4 bytes -|- length of metadata 4 bytes-|- meta-data list -|- file data blocks -|
 * inode: |- file index 4 bytes -|- file size 4 bytes-|- file name len 4 bytes -|- file name 200 bytes -|- list of data blocks (1024 * 4) -| -> 
 * file blocks: |- 1024 bytes -|
 * 
 */
public class NullFS implements Filesystem {

    private HDD disk;
    private int blockSize;
    private int fatSize;
    private int rootDirectorySize;
    
    // maximum length of file name (chars)
    private int maxLen4Filename = 25;
    
    // length for each file's meta-data
    // private int len4metadata;

    /**
     * Destructively initialise a disk with filesystem meta-data ready for use
     * @param hdd Target disk
     * @param blockSize Bytes per block
     * @param rootDirSize Bytes/Blocks reserved for root directory
     * @return A new filesystem instance now written to that disk
     */
    public static Filesystem format(HDD hdd, int blockSize, int rootDirSize) {
    	return new NullFS(hdd, 256, blockSize, rootDirSize);
    }

    /**
     * 
     * 
     * @param disk the total bytes on disk
     * @param fatSize the maximum bytes one file can contain
     * @param blockSize the block size
     * @param rootDirSize the maximum number of file t
     */
    private NullFS(HDD disk, int fatSize, int blockSize, int rootDirSize) {
        this.disk = disk;
        this.fatSize = fatSize;
        this.blockSize = blockSize;
        this.rootDirectorySize = rootDirSize;
        
        // erase data in blocks 0x00000000
        this.eraseDiskData(0, this.disk.capacity());
        
        // the space for file data = total size - heads
        int len4metadata = 4 + 4 + 4 + maxLen4Filename * 2 + this.fatSize * 4;
        int freeBlockLinks = this.disk.capacity() / this.blockSize;
        int bytes4blocks = this.disk.capacity() - 20 - freeBlockLinks - len4metadata * this.rootDirectorySize;
        int numOfBlocks = bytes4blocks/this.blockSize;
        
        if(numOfBlocks <= 0)
        	throw new RuntimeException("please expand the disk space");
        
        int used = this.disk.capacity() - bytes4blocks;
        double percent = used / this.disk.capacity() * 100;

        
        // print format information
        System.out.println("Format information:");
        System.out.println("free space for file data (size * number): " + this.blockSize * numOfBlocks);
        System.out.println("bytes for block recorder: " + freeBlockLinks);
        System.out.println("bytes for file inode: " + len4metadata * this.rootDirectorySize);
        System.out.println("bytes for block data: " + bytes4blocks);
        System.out.println("disk space usage: " + (this.disk.capacity() - bytes4blocks) + " / " + this.disk.capacity() + " = " + percent + "%");
        
        // write basic information on disk
        this.saveBytesFromInt(freeBlockLinks, 0); // block links length
        this.saveBytesFromInt(numOfBlocks, freeBlockLinks + 4); // total number of blocks
        this.saveBytesFromInt(0, freeBlockLinks + 8); // used number of blocks
        this.saveBytesFromInt(0, freeBlockLinks + 12); // number of files
        this.saveBytesFromInt(len4metadata, freeBlockLinks + 16); // size of metadata
        
        
        
        // ONLY for test
        //this.writeFileContent2Disk(new int[]{1,5}, "hello my name is shenyang hello my name is shenyang hello my name is shenyang".getBytes());
        //System.out.println(this.getFileContentByBlocks(new int[]{1,5}, "hello my name is shenyang hello my name is shenyang hello my name is shenyang".getBytes().length));
    }

    @Override
    public int newFile(byte[] contents) {
    	return this.newFile("untitle file",contents);
    }

    @Override
    public void deleteFile(int index) {
    	
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	int usedBlockNum = this.getIntValueFromDisk(freeBlockLinks + 8);
    	//int fileNum = this.getIntValueFromDisk(freeBlockLinks + 12);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);
    	int metaDataStart = 20 + freeBlockLinks; // start for inode list
    	// search in inode list for index
    	for(int i = 0; i < this.rootDirectorySize; i++){
    		metaDataStart += metadataLen * i;
    		if(this.getIntValueFromDisk(metaDataStart)==0)
				continue;
    		int fileIndex = this.getIntValueFromDisk(metaDataStart);
    		if(fileIndex == index){
    			// get its blocks
    			int fileSize = this.getIntValueFromDisk(metaDataStart + 4);
    			int blockNum = fileSize / this.blockSize;
    			if(fileSize % this.blockSize != 0) blockNum++;
    			int[] blocks = new int[blockNum];
    			for(int j = 0; j < blockNum; j++){
    				blocks[j] = this.getIntValueFromDisk(metaDataStart + 12 + this.maxLen4Filename * 2 + j*4);
    			}
    			
    			// erase its block data, write 0x00000000
    			byte[] nuc = new byte[fileSize];    			
    			this.writeFileContent2Disk(blocks, nuc);
    			
    			// erase the free links
    			for(int j = 0; j < blocks.length; ++j){
    				this.disk.seek(4 + blocks[j]);
    				this.disk.write((byte) 0x00000000);
    			}
    			
    			// erase the inode
    			this.eraseDiskData(metaDataStart, metaDataStart + metadataLen);
    			
    			// update disk head
    			//fileNum--;
    			usedBlockNum -= blocks.length;
    			this.saveBytesFromInt(usedBlockNum, freeBlockLinks + 8); // update used blocks
    	    	//this.saveBytesFromInt(fileNum, freeBlockLinks + 12); // increase file number
    			
    			break;
    		}
    	}
    }

    @Override
    public void dumpContents() {
        
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	//int totalBlockNum = this.getIntValueFromDisk(freeBlockLinks + 4);
    	//int usedBlockNum = this.getIntValueFromDisk(freeBlockLinks + 8);
    	int fileNum = this.getIntValueFromDisk(freeBlockLinks + 12);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);	  	
    	
    	if(fileNum <= 0){
    		System.out.println("no file saved yet!");
    	}else{
    		//System.out.println("file number: " + fileNum);
    		int metaDataStart = 20 + freeBlockLinks; // start for inode list
    		
    		// find files in inode list
    		for(int i = 0; i < this.rootDirectorySize; i++){
    			metaDataStart += metadataLen * i;
    			//System.out.println("newfile: " + metaDataStart);
    			if(this.getIntValueFromDisk(metaDataStart) > fileNum)
    				continue;
    			int fileIndex = this.getIntValueFromDisk(metaDataStart);
    			int fileSize = this.getIntValueFromDisk(metaDataStart + 4);
    			int fileNameLen = this.getIntValueFromDisk(metaDataStart + 8);
    			
    			// filename
    			//System.out.println(fileNameLen);
    			byte[] fileNameArray = new byte[fileNameLen];
    			for(int j = 0; j < fileNameLen; j++){
    				this.disk.seek(j + metaDataStart + 12);
    				fileNameArray[j] = this.disk.read();
    			}
    			
    			String fileName = new String(fileNameArray);
    			
    			// blocks
    			int blockNum = fileSize / this.blockSize;
    			if(fileSize % this.blockSize != 0) blockNum++;
    			int[] blocks = new int[blockNum];
    			String bs = "";
    			for(int j = 0; j < blockNum; j++){
    				blocks[j] = this.getIntValueFromDisk(metaDataStart + 12 + this.maxLen4Filename * 2 + j*4);
    				bs += blocks[j] + ",";
    			}
    			
    			// obtain content
    			String content = this.getFileContentByBlocks(blocks, fileSize);
    			
    			System.out.println("Store file, index: " + fileIndex + ", file name: " + fileName + ", content: " + content + ", blocks: " + bs);
    		}
    		
    		int total = this.getIntValueFromDisk(freeBlockLinks + 4);
    		int used = this.getIntValueFromDisk(freeBlockLinks + 8);
    		System.out.println("Usage (blocks): " + used + " / " + total);
    		
    	}
    	
    }

    @Override
    public int newFile(String filename, byte[] contents) {
        
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	int totalBlockNum = this.getIntValueFromDisk(freeBlockLinks + 4);
    	int usedBlockNum = this.getIntValueFromDisk(freeBlockLinks + 8);
    	int fileNum = this.getIntValueFromDisk(freeBlockLinks + 12);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);
    	
    	if(fileNum + 1 > this.rootDirectorySize){
    		System.out.println("no more space for new file");
    		return -1;
    	}
    	
    	if(filename.length() > this.maxLen4Filename){
    		System.out.println("too long file name for " + this.maxLen4Filename);
    		return -1;
    	}
    	
    	int fileSize = contents.length;
    	int fileNameLen = filename.getBytes().length;
    	int blockNeeded = fileSize / this.blockSize;
    	
    	// calculate the blocks needed
    	if(fileSize % this.blockSize != 0) blockNeeded++;
    	
    	if(blockNeeded > totalBlockNum - usedBlockNum){
    		System.out.println("no more space for new file");
    		return -1;
    	}
    	
    	// data blocks for this file
    	int[] blocks = new int[blockNeeded];
    	int bindex = 0;
    	
    	// search for free block in list
    	int bound = totalBlockNum + 4 - 1;
    	for(int i = 4; i < bound; ++i){
    		this.disk.seek(i);
    		if(this.disk.read() == 0x00000000){
    			//System.out.println("find free link:" + (i - 4) + " for file " + fileNum);
    			blocks[bindex++] = i - 4;
    			this.disk.seek(i);
    			this.disk.write((byte) 0x11111111);
    			usedBlockNum++;
    			
    			if(bindex >= blockNeeded)
    				break;
    		}
    	}
    	
    	
    	// update disk information
    	fileNum++;
    	this.saveBytesFromInt(usedBlockNum, freeBlockLinks + 8); // update used blocks
    	this.saveBytesFromInt(fileNum, freeBlockLinks + 12); // increase file number
    	
    	// write file metadata
    	int metaDataStart = 20 + freeBlockLinks;
    	int count = 0;
    	while(count < this.rootDirectorySize){
    		metaDataStart += metadataLen * count;
    		count++;
    		this.disk.seek(metaDataStart);
    		if(this.getIntValueFromDisk(metaDataStart) == 0){    			
    			break;
    		}
    	}
    	
    	// the metaDataStart for this file
    	this.saveBytesFromInt(fileNum, metaDataStart);
    	this.saveBytesFromInt(fileSize, metaDataStart + 4);
    	this.saveBytesFromInt(fileNameLen, metaDataStart + 8);
    	
    	// file name
    	byte[] fileNameBytes = filename.getBytes();
    	int start = metaDataStart + 12;
    	for(int i = 0; i < fileNameLen; ++i){
    		this.disk.seek(start + i);
    		this.disk.write(fileNameBytes[i]);
    	}
    	
    	// block list for this file
    	start = metaDataStart + 4 + 4 + 4 + maxLen4Filename * 2;
    	for(int i = 0; i < blocks.length; ++i){
    		this.saveBytesFromInt(blocks[i], start + 4 * i);
    	}    	
    	
    	// write data to disk
    	this.writeFileContent2Disk(blocks, contents);    	
    	return fileNum;
    }

    @Override
    public void deleteFile(String filename) {
    	
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	int usedBlockNum = this.getIntValueFromDisk(freeBlockLinks + 8);
    	//int fileNum = this.getIntValueFromDisk(freeBlockLinks + 12);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);
    	
    	int metaDataStart = 20 + freeBlockLinks; // start for inode list
    	
    	// search in inode list for index
    	for(int i = 0; i < this.rootDirectorySize; i++){
    		metaDataStart += metadataLen * i;
    		if(this.getIntValueFromDisk(metaDataStart)==0)
				continue;
    		
    		int fileNameLen = this.getIntValueFromDisk(metaDataStart + 8);
    		
    		// obtain the file name
    		byte[] fileNameBytes = new byte[fileNameLen];
    		for(int j = 0; j < fileNameLen; ++j){
    			this.disk.seek(metaDataStart + 12 + j);
    			fileNameBytes[j] = this.disk.read();
    		}
    		
    		String fileName = new String(fileNameBytes);
    		
    		//int fileIndex = this.getIntValueFromDisk(metaDataStart);
    		if(fileName.equalsIgnoreCase(filename)){
    			// get its blocks
    			int fileSize = this.getIntValueFromDisk(metaDataStart + 4);
    			int blockNum = fileSize / this.blockSize;
    			if(fileSize % this.blockSize != 0) blockNum++;
    			int[] blocks = new int[blockNum];
    			for(int j = 0; j < blockNum; j++){
    				blocks[j] = this.getIntValueFromDisk(metaDataStart + 12 + this.maxLen4Filename * 2 + j*4);
    			}
    			
    			// erase its block data, write 0x00000000
    			byte[] nuc = new byte[fileSize];    			
    			this.writeFileContent2Disk(blocks, nuc);
    			
    			// erase the free links
    			for(int j = 0; j < blocks.length; ++j){
    				this.disk.seek(4 + blocks[j]);
    				this.disk.write((byte) 0x00000000);
    			}
    			
    			// erase the inode
    			this.eraseDiskData(metaDataStart, metaDataStart + metadataLen);
    			
    			// update disk head
    			usedBlockNum -= blocks.length;
    			//fileNum--;
    			this.saveBytesFromInt(usedBlockNum, freeBlockLinks + 8); // update used blocks
    	    	//this.saveBytesFromInt(fileNum, freeBlockLinks + 12); // increase file number
    			
    			break;
    		}
    	}
    }
    
    
    /**
     * Read stored value as integer from the disk.
     * byte[start]byte[start+1]byte[start+2]byte[start+3]
     * 
     * @param start
     * @return
     */
    public int getIntValueFromDisk(int start){
    	disk.seek(start);
    	int i = (disk.read() << 24) & 0xFF000000;  
    	i |= (disk.read() << 16) & 0xFF0000;  
    	i |= (disk.read() << 8) & 0xFF00;  
    	i |= disk.read() & 0xFF;  
    	return i;  

    }
    
    /**
     * Read stored value as character from the disk.
     * byte[start]byte[start+1]
     * 
     * @param start
     * @return
     */
    public char getCharValueFromDisk(int start){
    	disk.seek(start);
    	char c = (char) ((disk.read() << 8) & 0xFF00L);  
    	c |= (char) (disk.read() & 0xFFL);  
    	return c;  

    }
    
    /**
     * Parse a integer value into byte array for recording.
     * Save to the disk.
     * 
     * @param value
     * @return
     */
    public void saveBytesFromInt(int i, int start){   	
    	// save to disk
    	disk.seek(start);
    	disk.write((byte) (i >>> 24));
    	disk.write((byte) (i >>> 16));
    	disk.write((byte) (i >>> 8));
    	disk.write((byte) i);
    }
    
    /**
     * Obtain file content by its blocks / only characters is permitted in file
     * 
     * @param blocks the block list
     * @param size the file size
     * @return
     */
    public String getFileContentByBlocks(int[] blocks, int size){
    	
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);
    	int dataBlockBegin = 20 + freeBlockLinks + metadataLen * this.rootDirectorySize;
    	
    	//StringBuffer sb = new StringBuffer();  
    	byte[] sb = new byte[size];
    	int index = 0;
    	
    	for(int i = 0; i < blocks.length; ++i){
    		int start = dataBlockBegin + blocks[i] * this.blockSize; // blocks begins with 0,1,2,3,4...
    		int pivot = 0;
    		int bound = this.blockSize;
    		if(size < this.blockSize) bound = size;
    		while(pivot < bound){  			
    			this.disk.seek(start + pivot);
    			//System.out.println("read from:" + (start+pivot));
    			sb[index++] = this.disk.read();
    			pivot += 1;
    		}
    		size = size - this.blockSize;
    	}
    	
    	return new String(sb);
    }
    
    /**
     * Write content to blocks specified
     * 
     * @param blocks
     * @param content
     */
    public void writeFileContent2Disk(int[] blocks, byte[] content){
    	
    	int freeBlockLinks = this.getIntValueFromDisk(0);
    	int metadataLen = this.getIntValueFromDisk(freeBlockLinks + 16);
    	
    	//System.out.println("content len:" + content.length);
    	int dataBlockBegin = 20 + freeBlockLinks + metadataLen * this.rootDirectorySize;
    	
    	int size = content.length;
    	
    	for(int i = 0; i < blocks.length; ++i){
    		int start = dataBlockBegin + blocks[i] * this.blockSize;
    		
    		//System.out.println("start at:" + start);
    		
    		int pivot = 0;
    		int bound = this.blockSize;
    		if(size < this.blockSize) bound = size;
    		while(pivot < bound){
    			this.disk.seek(start + pivot);
    			//System.out.println("write at:" + (start+pivot));
    			//System.out.println("write index:" + (content.length - size + pivot));
    			this.disk.write(content[content.length - size + pivot]);
    			pivot++;
    		}   		
    		
    		size = size - this.blockSize;
    	}
    }
    
    /**
     * Erase disks with 0x00000000
     * 
     * @param start
     * @param end
     */
    public void eraseDiskData(int start, int end){
    	for(int i = start; i < end; ++i){
    		this.disk.seek(i);
    		this.disk.write((byte) 0x00000000);
    	}
    }
    
}
