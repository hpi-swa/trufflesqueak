Class {
	#name : #PEEditAreaAbstract,
	#superclass : #Model,
	#instVars : [
		'fileEntry'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PEEditAreaAbstract class >> supports: aFileEntry [

	self subclassResponsibility
]

{ #category : #private }
PEEditAreaAbstract >> buildChildrenWith: aBuilder [

	"Implement either >>children or >>buildChildrenWith:"
	self subclassResponsibility
]

{ #category : #public }
PEEditAreaAbstract >> buildWith: aBuilder [

	^ aBuilder build: (PEPluggablePanelSpec new
		model: self;
		onClick: [self triggerEvent: #focus];
		children: ((self class includesSelector: #buildChildrenWith:)
			ifTrue: [self buildChildrenWith: aBuilder] 
			ifFalse: [#children]))
]

{ #category : #private }
PEEditAreaAbstract >> children [

	"Implement either >>children or >>buildChildrenWith:"
	self subclassResponsibility
]

{ #category : #public }
PEEditAreaAbstract >> convertLineEndings: newLineEndings [

	self subclassResponsibility
]

{ #category : #public }
PEEditAreaAbstract >> ensurePolyglotIsImported [

	self subclassResponsibility
]

{ #category : #private }
PEEditAreaAbstract >> fileEntry [

	^ fileEntry
]

{ #category : #private }
PEEditAreaAbstract >> fileEntry: aPCFileEntry [

	fileEntry := aPCFileEntry
]

{ #category : #public }
PEEditAreaAbstract >> isCodeEntry [

	^ false
]

{ #category : #public }
PEEditAreaAbstract >> lineEndingsName [

	self subclassResponsibility
]

{ #category : #private }
PEEditAreaAbstract >> openFile [

	self subclassResponsibility
]

{ #category : #public }
PEEditAreaAbstract >> openFile: aPCFileEntry [

	self fileEntry: aPCFileEntry.
	self openFile
]

{ #category : #public }
PEEditAreaAbstract >> replaceSelectionWith: aString [

	self subclassResponsibility
]
