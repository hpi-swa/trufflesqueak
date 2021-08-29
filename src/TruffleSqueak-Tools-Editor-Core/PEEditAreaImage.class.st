Class {
	#name : #PEEditAreaImage,
	#superclass : #PEEditAreaAbstract,
	#instVars : [
		'imageMorph'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PEEditAreaImage class >> supports: aFileEntry [

	^ (#(gif jpeg jpg png) includes: aFileEntry extension asLowercase) ifTrue: [10] ifFalse: [0]
]

{ #category : #private }
PEEditAreaImage >> children [

	^ {imageMorph}
]

{ #category : #public }
PEEditAreaImage >> convertLineEndings: newLineEndings [

]

{ #category : #public }
PEEditAreaImage >> ensurePolyglotIsImported [

]

{ #category : #'initialize-release' }
PEEditAreaImage >> initialize [

	super initialize.
	imageMorph := PESketchMorph new
		keepAspectRatio: true;
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1))
		yourself
]

{ #category : #public }
PEEditAreaImage >> lineEndingsName [

	^ '---'
]

{ #category : #private }
PEEditAreaImage >> openFile [

	imageMorph form: (Form fromBinaryStream: self fileEntry readBinary readStream)
]

{ #category : #public }
PEEditAreaImage >> replaceSelectionWith: aString [

]
