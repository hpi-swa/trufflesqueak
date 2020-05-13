file operations
createFile: aString

	self withFSDo: [:fs | fs writeStreamOn: aString].
	self flag: #todo. "Return created file"