"
I am a PluggableTextMorphPlus with the following extensions:
- I notify my model whenever my selection changes by performing setSelectionSelector on it.
- My text can be updated without being accepted by setEditTextSelector.
"
Class {
	#name : #PEPluggableCodeMorph,
	#superclass : #PluggableTextMorphPlus,
	#instVars : [
		'setSelectionSelector',
		'setEditTextSelector',
		'setLineEndingsSelector',
		'autoHeight',
		'onFocusSelector',
		'onKeyStrokeSelector'
	],
	#category : 'TruffleSqueak-Tools-Editor-Support'
}

{ #category : #updating }
PEPluggableCodeMorph >> adjustBackgroundColor [

	"Make my background a little transparent to that you can see the editor color"
	self color: ((self userInterfaceTheme color ifNil: [Color white]) alpha: 0.8)
]

{ #category : #updating }
PEPluggableCodeMorph >> applyUserInterfaceTheme [

	super applyUserInterfaceTheme.
	
	self adjustBackgroundColor
]

{ #category : #accessing }
PEPluggableCodeMorph >> autoHeight [

	^ autoHeight
]

{ #category : #accessing }
PEPluggableCodeMorph >> autoHeight: aBoolean [

	autoHeight := aBoolean.
	autoHeight 
		ifTrue: [	"self vScrollBarPolicy: #never" self flag: #todo "removing the scrollbar breaks autoHeight"]
		ifFalse: [self vScrollBarPolicy: #whenNeeded]
]

{ #category : #geometry }
PEPluggableCodeMorph >> extent: aPoint [

	autoHeight
		ifTrue: [	super extent: aPoint x@self minExtent y.
			self layoutChanged. owner layoutChanged] 
		ifFalse: [super extent: aPoint]
]

{ #category : #'event handling' }
PEPluggableCodeMorph >> handlesMouseWheel: evt [

	^ autoHeight ifTrue: [false] ifFalse: [super handlesMouseWheel: evt]
]

{ #category : #initialization }
PEPluggableCodeMorph >> initialize [

	autoHeight := false.
	super initialize.
	self adjustBackgroundColor
]

{ #category : #initialization }
PEPluggableCodeMorph >> initializeTextMorph [

	| padding |
	super initializeTextMorph.
	padding := 3.
	textMorph
		hResizing: #spaceFill;
		margins: ((padding)@0 corner: padding@0);
		setOnKeyStrokeSelector: onKeyStrokeSelector
]

{ #category : #'event handling' }
PEPluggableCodeMorph >> keyboardFocusChange: aBoolean [
	super keyboardFocusChange: aBoolean.
	(aBoolean and: [onFocusSelector notNil]) ifTrue: [
		onFocusSelector isSymbol
			ifTrue: [self model perform: onFocusSelector]
			ifFalse: [onFocusSelector value]]
]

{ #category : #layout }
PEPluggableCodeMorph >> minExtent [

	autoHeight ifFalse: [^ super minExtent].
	self flag: #todo. "Adding +2 removes the scroll bar but causes weird problems."
	^ super minExtent x@(textMorph extent y" + 2")
]

{ #category : #'event handling' }
PEPluggableCodeMorph >> mouseLeave: event [

	| result |
	result := super mouseLeave: event.
	self notifySelectionChanged.
	^ result
]

{ #category : #'model access' }
PEPluggableCodeMorph >> notifySelectionChanged [

	self setSelectionSelector ifNotNil: [:selector | 
		self model perform: selector with: selectionInterval]
]

{ #category : #accessing }
PEPluggableCodeMorph >> onFocusSelector [

	^ onFocusSelector
]

{ #category : #accessing }
PEPluggableCodeMorph >> onFocusSelector: anObject [

	onFocusSelector := anObject.
]

{ #category : #accessing }
PEPluggableCodeMorph >> onKeyStrokeSelector [

	^ onKeyStrokeSelector
]

{ #category : #accessing }
PEPluggableCodeMorph >> onKeyStrokeSelector: anObject [

	onKeyStrokeSelector := anObject.
]

{ #category : #'editor access' }
PEPluggableCodeMorph >> scrollSelectionIntoView: event [

	| result |
	result := super scrollSelectionIntoView: event.
	self notifySelectionChanged.
	^ result
]

{ #category : #accessing }
PEPluggableCodeMorph >> setEditTextSelector [

	^ setEditTextSelector
]

{ #category : #accessing }
PEPluggableCodeMorph >> setEditTextSelector: anObject [

	setEditTextSelector := anObject.
]

{ #category : #accessing }
PEPluggableCodeMorph >> setLineEndingsSelector [

	^ setLineEndingsSelector
]

{ #category : #accessing }
PEPluggableCodeMorph >> setLineEndingsSelector: aSymbol [

	setLineEndingsSelector := aSymbol
]

{ #category : #accessing }
PEPluggableCodeMorph >> setOnFocusSelector: aSymbol [

	onFocusSelector := aSymbol
]

{ #category : #accessing }
PEPluggableCodeMorph >> setOnKeyStrokeSelector: aSymbol [

	onKeyStrokeSelector := aSymbol
]

{ #category : #'model access' }
PEPluggableCodeMorph >> setSelection: sel [

	| result |
	result := super setSelection: sel.
	self notifySelectionChanged.
	^ result
]

{ #category : #accessing }
PEPluggableCodeMorph >> setSelectionSelector [

	^ setSelectionSelector
]

{ #category : #accessing }
PEPluggableCodeMorph >> setSelectionSelector: aSymbol [

	setSelectionSelector := aSymbol
]

{ #category : #private }
PEPluggableCodeMorph >> textMorphClass [

	^ PETextMorphForEditView
]

{ #category : #updating }
PEPluggableCodeMorph >> update: aSymbol [
	"overwrite super to check for a custom setEditTextSelector instead of simply listening for #editText"
	aSymbol ifNil: [^ self].
	aSymbol == self setEditTextSelector ifTrue: [
		self editString: (self model perform: self setEditTextSelector).
		^ self hasUnacceptedEdits: true].
	aSymbol == self setLineEndingsSelector ifTrue: [
		^ self textMorph editor lineEndings: (self model perform: self setLineEndingsSelector)].
	
	^ super update: aSymbol
]
