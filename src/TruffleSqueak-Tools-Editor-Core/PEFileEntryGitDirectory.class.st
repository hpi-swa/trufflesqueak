"
I represent a directory entry in a squit repo.
"
Class {
	#name : #PEFileEntryGitDirectory,
	#superclass : #PEFileEntryDirectory,
	#instVars : [
		'treeEntry',
		'path'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #'instance creation' }
PEFileEntryGitDirectory class >> on: aGitTreeEntry path: aFSPath [

	self assert: [aGitTreeEntry isInDirectoryMode].
	^ self new
		treeEntry: aGitTreeEntry;
		path: aFSPath;
		yourself
]

{ #category : #accessing }
PEFileEntryGitDirectory >> children [

	^ self treeEntry object entries values 
		"ignore submodules and symlinks"
		select: [:each | each isInDirectoryMode or: [each isInFileMode]]
		thenCollect: [:each | each isInDirectoryMode 
			ifTrue: [PEFileEntryGitDirectory on: each path: self path / each entryName] 
			ifFalse: [PEFileEntryGitFile on: each path: self path]]
]

{ #category : #'file operations' }
PEFileEntryGitDirectory >> createDirectory: aString [

	self withFSDo: [:fs | 
		fs createDirectory: aString.
		fs writeStreamOn: (aString asPathWith: fs) / '.gitkeep'].
	self flag: #todo. "Return created directory"
]

{ #category : #'file operations' }
PEFileEntryGitDirectory >> createFile: aString [

	self withFSDo: [:fs | fs writeStreamOn: aString].
	self flag: #todo. "Return created file"
]

{ #category : #'file operations' }
PEFileEntryGitDirectory >> delete [

	self withFSDo: [:fs | fs delete: '.']
]

{ #category : #accessing }
PEFileEntryGitDirectory >> fullPath [

	self shouldBeImplemented
]

{ #category : #accessing }
PEFileEntryGitDirectory >> hasChildren [
	"Git does not allow empty directories, therefore every directory must have children"
	^ true
]

{ #category : #accessing }
PEFileEntryGitDirectory >> name [

	^ self treeEntry entryName
]

{ #category : #private }
PEFileEntryGitDirectory >> path [

	^ path
]

{ #category : #private }
PEFileEntryGitDirectory >> path: aFSPath [

	path := aFSPath
]

{ #category : #'file operations' }
PEFileEntryGitDirectory >> rename: aString [

	self withFSDo: [:fs | fs rename: self treeEntry entryName to: aString]
]

{ #category : #accessing }
PEFileEntryGitDirectory >> supportsPolyglot [

	^ false
]

{ #category : #private }
PEFileEntryGitDirectory >> treeEntry [

	^ treeEntry
]

{ #category : #private }
PEFileEntryGitDirectory >> treeEntry: aGitTreeEntry [

	treeEntry := aGitTreeEntry
]

{ #category : #private }
PEFileEntryGitDirectory >> withFSDo: aBlock [

	| fs |
	fs := FileSystem onGit: self treeEntry repository workingDir.
	fs changeDirectory: (fs resolvePath: self path).
	aBlock value: fs.
	(UIManager default request: 'Commit Message') 
		ifNotEmpty: [:message | fs commit: message]
]
