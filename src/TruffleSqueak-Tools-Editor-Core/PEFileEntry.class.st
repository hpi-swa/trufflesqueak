"
I am an entry in the editor's file tree. I either represent a file or a directory and can be asked about my name and children.
"
Class {
	#name : #PEFileEntry,
	#superclass : #Object,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #accessing }
PEFileEntry >> children [
	self subclassResponsibility
]

{ #category : #'file operations' }
PEFileEntry >> delete [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> fullPath [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> hasChildren [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> isFile [
	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> name [
	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> printString [

	^ self fullPath
]

{ #category : #'file operations' }
PEFileEntry >> rename: aString [

	self subclassResponsibility
]

{ #category : #accessing }
PEFileEntry >> supportsPolyglot [

	^ true
]
