"
I represent an abstract directory entry.
"
Class {
	#name : #PEFileEntryDirectory,
	#superclass : #PEFileEntry,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'file operations' }
PEFileEntryDirectory >> createDirectory: aString [

	self subclassResponsibility
]

{ #category : #'file operations' }
PEFileEntryDirectory >> createFile: aString [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntryDirectory >> isFile [

	^ false
]
