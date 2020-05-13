file operations
delete

	self fileEntry containingDirectory deleteFileNamed: self fileEntry name 
		ifAbsent: [UIManager inform: 'Unable to delete file.']