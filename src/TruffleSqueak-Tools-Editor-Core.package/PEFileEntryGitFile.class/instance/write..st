file operations
write: aString

	self withFSDo: [:fs | | stream | 
		stream := fs writeStreamOn: self treeEntry entryName.
		stream
			truncate;
			nextPutAll: aString;
			close]