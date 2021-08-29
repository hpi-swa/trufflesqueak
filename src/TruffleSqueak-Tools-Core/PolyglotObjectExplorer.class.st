Class {
	#name : #PolyglotObjectExplorer,
	#superclass : #ObjectExplorer,
	#instVars : [
		'languageInfo'
	],
	#category : #'TruffleSqueak-Tools-Core'
}

{ #category : #defaults }
PolyglotObjectExplorer class >> nodeClass [

	^ PolyglotObjectExplorerWrapper
]

{ #category : #toolbuilder }
PolyglotObjectExplorer >> buildWith: builder [

	| windowSpec treeSpec textSpec buttonSpec buttonOffset tool |
	windowSpec := builder pluggableWindowSpec new.
	windowSpec
		model: self;
		children: OrderedCollection new;
		label: #label;
		extent: self initialExtent.
	
	treeSpec := builder pluggableTreeSpec new.
	treeSpec
		model: self;
		nodeClass: self class nodeClass;
		roots: #getList;
		keyPress: #explorerKey:from:event:;
		getSelected: #currentSelection;
		setSelected: #currentSelection:;
		setSelectedParent: #currentParent:;
		menu: #genericMenu:;
		dragItem: #getDragItem:;
		autoDeselect: false;
		columns: (PolyglotObjectExplorerWrapper showContentsInColumns
			ifTrue: [{
				[:listMorph | (listMorph filteredItems collect: [:item |
					item preferredWidthOfColumn: 1]) max].
				nil "take all the space"}]);
		frame: (0@0 corner: 1@0.71).
	windowSpec children add: treeSpec.

	buttonOffset := (Preferences standardButtonFont widthOfString: 'inspect') * 3/2.

	textSpec := builder pluggableCodePaneSpec new.
	textSpec
		model: self;
		getText: #expression;
		editText: #expression:;
		menu: #codePaneMenu:shifted:;
		help: #helpText;
		frame: (LayoutFrame fractions: (0@0.71 corner: 1@1) offsets: (0@0 corner: buttonOffset negated@0));
		stylerClass: PolyglotTextStyler.
	windowSpec children add: textSpec.

	buttonSpec := builder pluggableButtonSpec new
		model: self;
		label: 'inspect';
		action: #inspectObject;
		help: 'Switch to an inspector tool';
		frame: (LayoutFrame fractions: (1@0.71 corner: 1@1) offsets: (buttonOffset negated@0 corner: 0 @ 0)).
	windowSpec children add: buttonSpec.
	
	tool := builder build: windowSpec.
	self changed: #expandRootsRequested.
	^ tool
]

{ #category : #menus }
PolyglotObjectExplorer >> codePaneMenu: aMenu shifted: shifted [
	aMenu
		add: 'select language... (L)'
			target: self
			selector: #selectLanguage;
		add: 'set adaptive language selection... (l)'
			target: self
			selector: #setAdaptiveLanguageSelection;
		add: 'set default language... (h)'
			target: self
			selector: #setDefaultLanguage;
		add: 'set syntax highlighter... (h)'
			target: self
			selector: #setSyntaxHighlighter.
	^ StringHolder codePaneMenu: aMenu shifted: shifted
]

{ #category : #accessing }
PolyglotObjectExplorer >> currentSelection: anObject [
	super currentSelection: anObject.
	self updateAdaptiveLanguageSelection
]

{ #category : #accessing }
PolyglotObjectExplorer >> helpText [
	^ 'Evaluate {1} expressions for the current tree selection...'
		format: { self languageInfo getName }
]

{ #category : #'user interface' }
PolyglotObjectExplorer >> initialExtent [
	"Answer the desired extent for the receiver when it is first opened on the screen."

	^ 475 @ 300
]

{ #category : #accessing }
PolyglotObjectExplorer >> label [	
	^ self labelString
]

{ #category : #toolbuilder }
PolyglotObjectExplorer >> labelString [
	^ self object defaultLabelForPolyglotTools.
]

{ #category : #language }
PolyglotObjectExplorer >> languageId [
	^ self languageInfo getId asSymbol
]

{ #category : #language }
PolyglotObjectExplorer >> languageInfo [
	^ languageInfo ifNil: [
		languageInfo := (Polyglot useAdaptiveLanguageSelection and: [ Interop hasLanguage: self object ])
			ifTrue: [ (Interop getLanguageInfo: self object) ]
			ifFalse: [ Polyglot defaultLanguageInfo ] ]
]

{ #category : #language }
PolyglotObjectExplorer >> languageInfo: aLanguageInfo [
	languageInfo == aLanguageInfo ifTrue: [ ^ self ].
	languageInfo := aLanguageInfo.
	self changed: #style.
]

{ #category : #language }
PolyglotObjectExplorer >> selectLanguage [
	self languageInfo: Polyglot chooseLanguageInfo
]

{ #category : #language }
PolyglotObjectExplorer >> setAdaptiveLanguageSelection [
	Polyglot askForAdaptiveLanguageSelection.
	self updateAdaptiveLanguageSelection
]

{ #category : #language }
PolyglotObjectExplorer >> setDefaultLanguage [
	Polyglot askForDefaultLanguageInfo.
	self languageInfo: Polyglot defaultLanguageInfo
]

{ #category : #menus }
PolyglotObjectExplorer >> setSyntaxHighlighter [
	Polyglot askForRougeStyler.
	self changed: #style
]

{ #category : #language }
PolyglotObjectExplorer >> updateAdaptiveLanguageSelection [
	(Polyglot useAdaptiveLanguageSelection and: [
			Interop hasLanguage: self object ]) ifTrue: [
		self languageInfo: (Interop getLanguageInfo: self object ) ]
]
