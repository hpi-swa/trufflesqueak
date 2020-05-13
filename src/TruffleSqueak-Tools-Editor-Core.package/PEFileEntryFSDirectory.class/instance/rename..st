file operations
rename: aString

	| ref |
	ref := self fileDirectory asFSReference.
	ref renameTo: ref parent / aString