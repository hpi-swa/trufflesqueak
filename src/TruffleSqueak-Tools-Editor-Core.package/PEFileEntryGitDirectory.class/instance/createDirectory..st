file operations
createDirectory: aString

	self withFSDo: [:fs | 
		fs createDirectory: aString.
		fs writeStreamOn: (aString asPathWith: fs) / '.gitkeep'].
	self flag: #todo. "Return created directory"