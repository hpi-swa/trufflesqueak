Class {
	#name : #PEFileEntryTestFile,
	#superclass : #PEFileEntryFile,
	#instVars : [
		'lastModified',
		'parent',
		'size',
		'content',
		'children',
		'fullPath'
	],
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #'instance creation' }
PEFileEntryTestFile class >> forPath: aString [

	| path |
	path := aString copyReplaceAll: '\' with: '/'.
	^ PEFileEntryTestFile allInstances detect: [:each | each fullPath = path]
]

{ #category : #accessing }
PEFileEntryTestFile >> children [

	^ {}
]

{ #category : #accessing }
PEFileEntryTestFile >> content: anObject [

	content := anObject
]

{ #category : #accessing }
PEFileEntryTestFile >> extension [

	^ UnixFileDirectory extensionFor: self fullPath
]

{ #category : #accessing }
PEFileEntryTestFile >> fullPath [

	^ fullPath
]

{ #category : #accessing }
PEFileEntryTestFile >> fullPath: aString [

	fullPath := aString
]

{ #category : #accessing }
PEFileEntryTestFile >> initialize [

	lastModified := DateAndTime fromString: '01/02/2003 14:15'
]

{ #category : #accessing }
PEFileEntryTestFile >> lastModified: anObject [

	lastModified := anObject
]

{ #category : #accessing }
PEFileEntryTestFile >> name [

	UnixFileDirectory splitName: self fullPath to: [:path :name | ^ name]
]

{ #category : #accessing }
PEFileEntryTestFile >> parent [

	^ parent
]

{ #category : #accessing }
PEFileEntryTestFile >> parent: anObject [

	parent := anObject
]

{ #category : #'file operations' }
PEFileEntryTestFile >> read [

	^ content
]

{ #category : #accessing }
PEFileEntryTestFile >> size: anInteger [

	size := anInteger
]
