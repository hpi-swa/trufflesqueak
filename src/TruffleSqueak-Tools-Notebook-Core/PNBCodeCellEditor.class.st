Class {
	#name : #PNBCodeCellEditor,
	#superclass : #TextMorph,
	#instVars : [
		'codeCell'
	],
	#category : 'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #'instance creation' }
PNBCodeCellEditor class >> for: aCodeCell [
	^ self new codeCell: aCodeCell; yourself
]

{ #category : #actions }
PNBCodeCellEditor >> applyStyle [
	RougeStyler default style: self contents language: self codeCell languageID.
	self paragraph composeAll.
	self editor recomputeSelection.
	self updateFromParagraph.
	self editor blinkParen
]

{ #category : #accessing }
PNBCodeCellEditor >> codeCell [

	^ codeCell
]

{ #category : #accessing }
PNBCodeCellEditor >> codeCell: anObject [

	codeCell := anObject.
]

{ #category : #accessing }
PNBCodeCellEditor >> contents: aString [
	super contents: aString.
	Project current addDeferredUIMessage: [ self applyStyle ].
]

{ #category : #accessing }
PNBCodeCellEditor >> editor [
	^ super editor model: self; yourself
]

{ #category : #actions }
PNBCodeCellEditor >> evaluate [
	^ self evaluateExpression: self editor text.
]

{ #category : #actions }
PNBCodeCellEditor >> evaluateExpression:	aText [
	| code bindings |
	self codeCell isMarkdown ifTrue: [ ^ nil ].
	code := aText asString.
	bindings := self codeCell cellContainer bindings.
	^ (code beginsWith: '%ggplot2')
		ifTrue: [ (PNBRPlotMorph
					extent: (self width - 10)@(self width)
					plotCode: (code allButFirst: 9))
					bindings: bindings;
					yourself ]
		ifFalse: [ Polyglot eval: self codeCell languageID string: code
					names: #('bindings') arguments: {bindings} ]
]

{ #category : #'event handling' }
PNBCodeCellEditor >> getMenu: shiftKeyState [ 
	| menu |
	menu := (super getMenu: shiftKeyState) copy.
	menu
		addLine;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.
	^ menu
]

{ #category : #editing }
PNBCodeCellEditor >> hasUnacceptedEdits: aBoolean [
	aBoolean ifTrue: [ self triggerStyle ]
]

{ #category : #testing }
PNBCodeCellEditor >> indicateKeyboardFocus [
	^ false
]

{ #category : #initialization }
PNBCodeCellEditor >> initialize [
	super initialize.
	self
		setProperty: #defaultContents toValue: '' asText;
		borderColor: Color gray;
		hResizing: #spaceFill;
		margins: 2@2
]

{ #category : #'event handling' }
PNBCodeCellEditor >> setSyntaxHighlighter [
	Polyglot askForRougeStyler.
	self class allInstances do: [ :ea | ea triggerStyle ]
]

{ #category : #actions }
PNBCodeCellEditor >> triggerStyle [
	self removeAlarm: #applyStyle.
	self addAlarm: #applyStyle after: 300
]
