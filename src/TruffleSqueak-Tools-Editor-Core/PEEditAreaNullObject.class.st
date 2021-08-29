Class {
	#name : #PEEditAreaNullObject,
	#superclass : #PEEditAreaAbstract,
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PEEditAreaNullObject class >> supports: aFileEntry [

	^ 0
]

{ #category : #public }
PEEditAreaNullObject >> children [

	^ {}
]

{ #category : #public }
PEEditAreaNullObject >> convertLineEndings: newLineEndings [
]

{ #category : #public }
PEEditAreaNullObject >> ensurePolyglotIsImported [

]

{ #category : #public }
PEEditAreaNullObject >> lineEndingsName [

	^ 'unknown'
]

{ #category : #public }
PEEditAreaNullObject >> openFile [
]

{ #category : #public }
PEEditAreaNullObject >> replaceSelectionWith: aString [

]
