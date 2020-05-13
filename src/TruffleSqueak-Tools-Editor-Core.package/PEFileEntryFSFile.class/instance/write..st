file operations
write: aString

	self fileEntry readWriteStream
		truncate;
		nextPutAll: aString