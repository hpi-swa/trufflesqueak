Class {
	#name : #PEEditAreaMagicMouse,
	#superclass : #PEEditAreaAbstract,
	#instVars : [
		'browser'
	],
	#category : 'TruffleSqueak-Tools-Editor-Core'
}

{ #category : #public }
PEEditAreaMagicMouse class >> supports: aFileEntry [

	Smalltalk at: #MMBrowserMorph ifAbsent: [^ -100].
	^ (#(gif jpeg jpg png mp4 mp3 svg pdf wav ogg) includes: aFileEntry extension asLowercase) ifTrue: [5] ifFalse: [0]
]

{ #category : #private }
PEEditAreaMagicMouse >> children [

	^ {browser}
]

{ #category : #public }
PEEditAreaMagicMouse >> convertLineEndings: newLineEndings [

]

{ #category : #public }
PEEditAreaMagicMouse >> ensurePolyglotIsImported [

]

{ #category : #'initialize-release' }
PEEditAreaMagicMouse >> initialize [

	super initialize.
	browser := (Smalltalk at: #MMBrowserMorph) new
		layoutFrame: (LayoutFrame fractions: (0@0 extent: 1@1))
		yourself
]

{ #category : #public }
PEEditAreaMagicMouse >> lineEndingsName [

	^ '---'
]

{ #category : #private }
PEEditAreaMagicMouse >> openFile [

	| url |
	url := 'file://', self fileEntry fullPath.
	browser isConnected
		ifTrue: [browser changeLocation: url]
		ifFalse: [browser connect: url]
]

{ #category : #public }
PEEditAreaMagicMouse >> replaceSelectionWith: aString [

]
