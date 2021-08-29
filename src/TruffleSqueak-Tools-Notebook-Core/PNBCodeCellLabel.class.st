Class {
	#name : #PNBCodeCellLabel,
	#superclass : #PluggableButtonMorph,
	#instVars : [
		'codeCell',
		'colorForLanguageID'
	],
	#category : 'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #'instance creation' }
PNBCodeCellLabel class >> for: aCodeCell [
	^ self new codeCell: aCodeCell; refresh; yourself
]

{ #category : #accessing }
PNBCodeCellLabel >> codeCell [

	^ codeCell
]

{ #category : #accessing }
PNBCodeCellLabel >> codeCell: anObject [

	codeCell := anObject.
]

{ #category : #'events-processing' }
PNBCodeCellLabel >> handleMouseDown: anEvent [
	| menu action |
	menu := CustomMenu new.
	menu add: PolyglotNotebook markdownName action: Polyglot languageIdMarkdown.
	menu addLine.
	
	Polyglot availableLanguages do: [:ea |
		menu add: ea getName asString action: ea getId asString ].
	menu addLine.
	menu add: 'run cell...' action: #run.
	menu addLine.
	menu add: 'up...' action: #up.
	menu add: 'down...' action: #down.
	menu addLine.
	menu add: 'remove...' action: #remove.
	anEvent wasHandled: true.
	action := menu startUp: Polyglot languageIdMarkdown.
	action = #run ifTrue: [ self codeCell run. ^ self ].
	action = #up ifTrue: [ self codeCell up. ^ self ].
	action = #down ifTrue: [ self codeCell down. ^ self ].
	action = #remove ifTrue: [ self codeCell remove. ^ self ].
	self codeCell languageID: action.
	self refresh.
	
]

{ #category : #'event handling' }
PNBCodeCellLabel >> handlesMouseOver: evt [

	^ false
]

{ #category : #'event handling' }
PNBCodeCellLabel >> handlesMouseOverDragging: evt [

	^ false
]

{ #category : #testing }
PNBCodeCellLabel >> indicateKeyboardFocus [
	^ false
]

{ #category : #initialization }
PNBCodeCellLabel >> initialize [
	super initialize.
	self 
		borderWidth: 0;
	  	hResizing: #spaceFill;
		height: TextStyle defaultFont height * 1.8;
		target: self
]

{ #category : #helpers }
PNBCodeCellLabel >> labelFor: aLanguageID [
	| languageName result |
	languageName := self codeCell isMarkdown
		ifTrue: [ PolyglotNotebook markdownName ]
		ifFalse: [(Polyglot languageNameFor: self codeCell languageID) ].
	result := Morph new
		color: Color transparent;
		layoutPolicy: TableLayout new;
		cellInset: 4;
		listDirection: #leftToRight;
	  hResizing: #shrinkWrap;
	  vResizing: #shrinkWrap.
	result addMorph: languageName asMorph.
	result addMorph: (Polyglot iconFor: aLanguageID) asMorph.
	^ result
]

{ #category : #actions }
PNBCodeCellLabel >> refresh [
	| cellColor |
	self label: (self labelFor: self codeCell languageID).
	cellColor := (PolyglotNotebook colorFor: self codeCell languageID).
	self color: cellColor.
	self codeCell borderColor: cellColor
]
