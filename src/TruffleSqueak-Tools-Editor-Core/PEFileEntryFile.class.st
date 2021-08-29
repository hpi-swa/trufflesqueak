"
I represent an abstract file entry.
"
Class {
	#name : #PEFileEntryFile,
	#superclass : #PEFileEntry,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PEFileEntryFile >> extension [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntryFile >> isFile [

	^ true
]

{ #category : #accessing }
PEFileEntryFile >> lastModified [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntryFile >> parent [

	self subclassResponsibility
]

{ #category : #'file operations' }
PEFileEntryFile >> read [

	self subclassResponsibility
]

{ #category : #'file operations' }
PEFileEntryFile >> readBinary [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntryFile >> size [

	self subclassResponsibility
]

{ #category : #'file operations' }
PEFileEntryFile >> write: aString [

	self subclassResponsibility
]
