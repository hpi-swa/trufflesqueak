Class {
	#name : #PEFileEntryTestDirectory,
	#superclass : #PEFileEntryDirectory,
	#instVars : [
		'fullPath',
		'children'
	],
	#category : 'TruffleSqueak-Tools-Editor-Tests'
}

{ #category : #accessing }
PEFileEntryTestDirectory >> children [

	^ children
]

{ #category : #accessing }
PEFileEntryTestDirectory >> children: anObject [

	children := anObject
]

{ #category : #accessing }
PEFileEntryTestDirectory >> fullPath [

	^ fullPath
]

{ #category : #accessing }
PEFileEntryTestDirectory >> fullPath: anObject [

	fullPath := anObject
]

{ #category : #accessing }
PEFileEntryTestDirectory >> hasChildren [

	^ self children notEmpty
]

{ #category : #accessing }
PEFileEntryTestDirectory >> name [

	UnixFileDirectory splitName: self fullPath to: [:path :name | ^ name]
]
