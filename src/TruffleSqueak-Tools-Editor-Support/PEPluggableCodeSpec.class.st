"
A text editor that is able to notify its model when the selection changes and is able to be updated without being accepted.
"
Class {
	#name : #PEPluggableCodeSpec,
	#superclass : #PluggableTextSpec,
	#instVars : [
		'setSelection',
		'setEditText',
		'styler',
		'setLineEndings',
		'autoHeight',
		'onActive',
		'onFocus',
		'onKeyStroke'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #accessing }
PEPluggableCodeSpec >> autoHeight [

	^ autoHeight
]

{ #category : #accessing }
PEPluggableCodeSpec >> autoHeight: aBoolean [

	autoHeight := aBoolean
]

{ #category : #building }
PEPluggableCodeSpec >> buildWith: aBuilder [

	^ aBuilder buildPCPluggableCode: self
]

{ #category : #'initialize-release' }
PEPluggableCodeSpec >> initialize [

	super initialize.
	autoHeight := false
]

{ #category : #accessing }
PEPluggableCodeSpec >> onFocus [

	^ onFocus
]

{ #category : #accessing }
PEPluggableCodeSpec >> onFocus: aSymbol [

	onFocus := aSymbol
]

{ #category : #accessing }
PEPluggableCodeSpec >> onKeyStroke [

	^ onKeyStroke
]

{ #category : #accessing }
PEPluggableCodeSpec >> onKeyStroke: aSymbol [

	onKeyStroke := aSymbol
]

{ #category : #accessing }
PEPluggableCodeSpec >> setEditText [

	^ setEditText
]

{ #category : #accessing }
PEPluggableCodeSpec >> setEditText: aSymbol [

	setEditText := aSymbol
]

{ #category : #accessing }
PEPluggableCodeSpec >> setLineEndings [

	^ setLineEndings
]

{ #category : #accessing }
PEPluggableCodeSpec >> setLineEndings: aString [

	setLineEndings := aString
]

{ #category : #accessing }
PEPluggableCodeSpec >> setSelection [

	^ setSelection
]

{ #category : #accessing }
PEPluggableCodeSpec >> setSelection: aSymbol [

	setSelection := aSymbol
]

{ #category : #accessing }
PEPluggableCodeSpec >> styler [

	^ styler
]

{ #category : #accessing }
PEPluggableCodeSpec >> styler: aSHTextStyler [

	styler := aSHTextStyler
]
