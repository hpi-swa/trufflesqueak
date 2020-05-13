accessing
lastModified

	| currentFile |
	currentFile := self fileEntry containingDirectory asFSReference / self fileEntry name.
	Smalltalk version = 'Squeak5.2'
		ifTrue: [^ DateAndTime fromSeconds: currentFile modificationTime]
		ifFalse: [
		^ DateAndTime
			utcSeconds: currentFile modificationTime 
			offset: DateAndTime epochOffset negated]