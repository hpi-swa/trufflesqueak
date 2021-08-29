"
I represent a file entry in a squit repo.
"
Class {
	#name : #PEFileEntryGitFile,
	#superclass : #PEFileEntryFile,
	#instVars : [
		'treeEntry',
		'path'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'instance creation' }
PEFileEntryGitFile class >> on: aGitTreeEntry path: aFSPath [

	self assert: [aGitTreeEntry isInFileMode].
	^ self new
		treeEntry: aGitTreeEntry;
		path: aFSPath
		yourself
]

{ #category : #accessing }
PEFileEntryGitFile >> children [

	^ Array new
]

{ #category : #'file operations' }
PEFileEntryGitFile >> delete [

	self withFSDo: [:fs | fs delete: self treeEntry entryName]
]

{ #category : #accessing }
PEFileEntryGitFile >> extension [

	^ FileDirectory extensionFor: self name
]

{ #category : #accessing }
PEFileEntryGitFile >> fullPath [

	^ self withFSDo: [:fs | 
		^ (fs repository repository workingDir / self treeEntry entryName) asString]
]

{ #category : #accessing }
PEFileEntryGitFile >> hasChildren [

	^ false
]

{ #category : #accessing }
PEFileEntryGitFile >> lastModified [


	^ self withFSDo: [:fs | | entry |
		entry := (fs workingDirectory / self treeEntry entryName).
		self flag: #todo. "Modification time is always 0. We would need to search the git commit that last modified/created this file". 
		^ DateAndTime now]
]

{ #category : #accessing }
PEFileEntryGitFile >> name [

	^ self treeEntry entryName
]

{ #category : #accessing }
PEFileEntryGitFile >> parent [

	self flag: #todo.
	self notYetImplemented
]

{ #category : #private }
PEFileEntryGitFile >> path [

	^ path
]

{ #category : #private }
PEFileEntryGitFile >> path: aFSPath [

	path := aFSPath
]

{ #category : #'file operations' }
PEFileEntryGitFile >> read [

	^ self treeEntry object bytes asString
]

{ #category : #'file operations' }
PEFileEntryGitFile >> readBinary [

	^ self treeEntry object bytes
]

{ #category : #'file operations' }
PEFileEntryGitFile >> rename: aString [

	self withFSDo: [:fs | fs rename: self treeEntry entryName to: aString]
]

{ #category : #accessing }
PEFileEntryGitFile >> size [

	^ self treeEntry object bytes size
]

{ #category : #accessing }
PEFileEntryGitFile >> supportsPolyglot [

	^ false
]

{ #category : #private }
PEFileEntryGitFile >> treeEntry [

	^ treeEntry
]

{ #category : #private }
PEFileEntryGitFile >> treeEntry: aGitTreeEntry [

	treeEntry := aGitTreeEntry
]

{ #category : #private }
PEFileEntryGitFile >> withFSDo: aBlock [

	| fs |
	fs := FileSystem onGit: self treeEntry repository workingDir.
	fs changeDirectory: (fs resolvePath: self path).
	aBlock value: fs.
	(UIManager default request: 'Commit Message') 
		ifNotEmpty: [:message | fs commit: message]
]

{ #category : #'file operations' }
PEFileEntryGitFile >> write: aString [

	self withFSDo: [:fs | | stream | 
		stream := fs writeStreamOn: self treeEntry entryName.
		stream
			truncate;
			nextPutAll: aString;
			close]
]
