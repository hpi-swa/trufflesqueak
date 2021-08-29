"
I represent a file entry in the filesystem.
"
Class {
	#name : #PEFileEntryFSFile,
	#superclass : #PEFileEntryFile,
	#instVars : [
		'fileEntry'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'instance creation' }
PEFileEntryFSFile class >> forPath: aString [

	| directoryEntry |
	directoryEntry := FileDirectory
		splitName: aString
		to: [:dirPath :localName | (FileDirectory on: dirPath) directoryEntryFor: localName].
	^ self on: directoryEntry
]

{ #category : #'instance creation' }
PEFileEntryFSFile class >> on: aDirectoryEntryFile [

	^ self new
		fileEntry: aDirectoryEntryFile;
		yourself
]

{ #category : #accessing }
PEFileEntryFSFile >> children [

	^ Array new
]

{ #category : #'file operations' }
PEFileEntryFSFile >> delete [

	self fileEntry containingDirectory deleteFileNamed: self fileEntry name 
		ifAbsent: [UIManager inform: 'Unable to delete file.']
]

{ #category : #accessing }
PEFileEntryFSFile >> extension [

	^ self fileEntry extension
]

{ #category : #private }
PEFileEntryFSFile >> fileEntry [

	^ fileEntry
]

{ #category : #private }
PEFileEntryFSFile >> fileEntry: aDirectoryEntryFile [

	fileEntry := aDirectoryEntryFile
]

{ #category : #accessing }
PEFileEntryFSFile >> fullPath [

	^ self fileEntry fullName
]

{ #category : #accessing }
PEFileEntryFSFile >> hasChildren [

	^ false
]

{ #category : #accessing }
PEFileEntryFSFile >> lastModified [

	| currentFile |
	currentFile := self fileEntry containingDirectory asFSReference / self fileEntry name.
	Smalltalk version = 'Squeak5.2'
		ifTrue: [^ DateAndTime fromSeconds: currentFile modificationTime]
		ifFalse: [
		^ DateAndTime
			utcSeconds: currentFile modificationTime 
			offset: DateAndTime epochOffset negated]
]

{ #category : #accessing }
PEFileEntryFSFile >> name [

	^ self fileEntry name
]

{ #category : #accessing }
PEFileEntryFSFile >> parent [

	^ PEFileEntryFSDirectory on: self fileEntry containingDirectory
]

{ #category : #'file operations' }
PEFileEntryFSFile >> read [

	^ self fileEntry readStream upToEnd
]

{ #category : #'file operations' }
PEFileEntryFSFile >> readBinary [

	^ self fileEntry readStream 
		binary;
		upToEnd
]

{ #category : #'file operations' }
PEFileEntryFSFile >> rename: aString [

	self fileEntry containingDirectory rename: self fileEntry name toBe: aString
]

{ #category : #accessing }
PEFileEntryFSFile >> size [

	^ self fileEntry size
]

{ #category : #'file operations' }
PEFileEntryFSFile >> write: aString [

	self fileEntry readWriteStream
		truncate;
		nextPutAll: aString
]
