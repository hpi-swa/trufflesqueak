class initialization
ensureInitialized
	(Smalltalk specialObjectsArray at: self specialObjectsArrayIndex) ifNil: [
		self notify: self name, ' is not registered in the specialObjectsArray. Please click "Proceed" and then save and reopen the image for polyglot access.'.
		self initialize ]