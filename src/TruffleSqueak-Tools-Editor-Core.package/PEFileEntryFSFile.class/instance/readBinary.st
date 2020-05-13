file operations
readBinary

	^ self fileEntry readStream 
		binary;
		upToEnd