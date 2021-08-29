"
I represent a directory entry in the filesystem.
"
Class {
	#name : #PEFileEntryFSDirectory,
	#superclass : #PEFileEntryDirectory,
	#instVars : [
		'fileDirectory'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'instance creation' }
PEFileEntryFSDirectory class >> forPath: aString [

	^ self on: (FileDirectory on: aString)
]

{ #category : #'instance creation' }
PEFileEntryFSDirectory class >> on: aFileDirectory [

	^ self new
		fileDirectory: aFileDirectory;
		yourself
]

{ #category : #accessing }
PEFileEntryFSDirectory >> children [

	^ self fileDirectory entries collect: [:each | each isDirectory 
		ifTrue: [PEFileEntryFSDirectory on: each asFileDirectory]
		ifFalse: [PEFileEntryFSFile on: each]]
]

{ #category : #'file operations' }
PEFileEntryFSDirectory >> createDirectory: aString [

	(self fileDirectory directoryExists: aString) ifFalse: [
		self fileDirectory createDirectory: aString].
	^ PEFileEntryFSDirectory on: (self fileDirectory directoryNamed: aString)
]

{ #category : #'file operations' }
PEFileEntryFSDirectory >> createFile: aString [

	self fileDirectory fileNamed: aString.
	^ PEFileEntryFSFile on: (self fileDirectory directoryEntryFor: aString)
]

{ #category : #'file operations' }
PEFileEntryFSDirectory >> delete [

	self fileDirectory asFSReference deleteAll
]

{ #category : #private }
PEFileEntryFSDirectory >> fileDirectory [

	^ fileDirectory
]

{ #category : #private }
PEFileEntryFSDirectory >> fileDirectory: aFileDirectory [

	fileDirectory := aFileDirectory
]

{ #category : #accessing }
PEFileEntryFSDirectory >> fullPath [

	^ self fileDirectory name
]

{ #category : #accessing }
PEFileEntryFSDirectory >> hasChildren [

	^ self fileDirectory hasEntries
]

{ #category : #accessing }
PEFileEntryFSDirectory >> name [

	^ self fileDirectory directoryEntry name
]

{ #category : #'file operations' }
PEFileEntryFSDirectory >> rename: aString [

	| ref |
	ref := self fileDirectory asFSReference.
	ref renameTo: ref parent / aString
]
