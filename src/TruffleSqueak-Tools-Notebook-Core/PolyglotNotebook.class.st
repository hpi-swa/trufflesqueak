Class {
	#name : #PolyglotNotebook,
	#superclass : #Model,
	#instVars : [
		'cellContainer',
		'currentSelection',
		'showLanguageScopes'
	],
	#category : #'TruffleSqueak-Tools-Notebook-Core'
}

{ #category : #helpers }
PolyglotNotebook class >> colorFor: aLanguageID [
	| id |
	id := aLanguageID asLowercase.
	id = Polyglot languageIdJS ifTrue: [^ Color fromString: '#FFEB3B'].
	id = Polyglot languageIdLLVM ifTrue: [^ Color fromString: '#CE93D8'].
	id = Polyglot languageIdMarkdown ifTrue: [^ Color fromString: '#E0E0E0' ].
	id = Polyglot languageIdPython ifTrue: [^ Color fromString: '#039BE5'].
	id = Polyglot languageIdR ifTrue: [^ Color fromString: '#81D4FA'].
	id = Polyglot languageIdRuby ifTrue: [^ Color fromString: '#E57373'].
	id = Polyglot languageIdSmalltalk ifTrue: [^ Color fromString: '#9CCC65'].
	^ Color gray
]

{ #category : #helpers }
PolyglotNotebook class >> explorerMorphFor: anObject [
	| explorerView |
	explorerView := (ToolBuilder build: (PolyglotObjectExplorer explore: anObject))
		findA: PluggableTreeMorph.
	^ explorerView
]

{ #category : #'class initialization' }
PolyglotNotebook class >> initialize [
	"self initialize"
	super initialize.
	self registerInWorldMenu
]

{ #category : #constants }
PolyglotNotebook class >> markdownName [
	^ Polyglot languageIdMarkdown capitalized
]

{ #category : #toolbuilder }
PolyglotNotebook class >> open [
	^ ToolBuilder open: self
]

{ #category : #'world menu' }
PolyglotNotebook class >> registerInWorldMenu [
	self environment at: #TheWorldMenu ifPresent: [ :class |
		class registerOpenCommand: (Array 
			with: 'Polyglot Notebook' 
			with: (Array
				with: self
				with: #open)) ].
]

{ #category : #actions }
PolyglotNotebook >> actionAddCell [
	self cellContainer addCodeCell
]

{ #category : #actions }
PolyglotNotebook >> actionLoad [
	self cellContainer load
]

{ #category : #actions }
PolyglotNotebook >> actionRunAll [
	self cellContainer runAll
]

{ #category : #actions }
PolyglotNotebook >> actionSave [
	self cellContainer save
]

{ #category : #toolbuilder }
PolyglotNotebook >> buildCodePaneWith: builder [
	| textSpec |
	textSpec := builder pluggableScrollPaneSpec new.
	textSpec
		layout: #vertical;
		borderWidth: 0;
		morph: self cellContainer.
	^textSpec
]

{ #category : #toolbuilder }
PolyglotNotebook >> buildSidebar: builder [
	| panelSpec treeSpec |

	panelSpec := builder pluggablePanelSpec new
		"layout: #vertical;"
		children: OrderedCollection new;
		yourself.
	
	treeSpec := builder pluggableTreeSpec new.
	treeSpec
		model: self;
		nodeClass: PNBBindingsWrapper;
		roots: #getList;
		keyPress: #explorerKey:from:event:;
		getSelected: #currentSelection;
		setSelected: #currentSelection:;
		"setSelectedParent: #currentParent:;"
		menu: #genericMenu:;
		"dragItem: #getDragItem:;"
		columns: (PNBBindingsWrapper showContentsInColumns
			ifTrue: [{
				[:listMorph | (listMorph filteredItems collect: [:item |
					item preferredWidthOfColumn: 1]) max].
				nil "take all the space"}]);
		autoDeselect: false;
		frame: (LayoutFrame
			fractions: (0@0 corner: 1@1)
			offsets: (0@ (self buttonHeight + 2) corner: 0@0)).
	panelSpec children addAll: {self buildSwitchesWith: builder. treeSpec}.	
	^ panelSpec
]

{ #category : #toolbuilder }
PolyglotNotebook >> buildSwitchesWith: builder [
	"Build the instance/comment/class switch"

	| panelSpec r a l s |
	panelSpec := builder pluggablePanelSpec new
		layout: #horizontal;
		spacing: -1;
		children: OrderedCollection new;
		yourself.

	r := builder pluggableButtonSpec new.
	r 
			model: self;
			"horizontalResizing: #shrinkWrap;"
			label: 'Run all';
			help: 'Run all code cells'; 
			action: #actionRunAll.

	a := builder pluggableButtonSpec new.
	a 
			model: self;
			label: 'Add cell';
			help: 'Add a new code cell';
			action: #actionAddCell.
			
	l := builder pluggableButtonSpec new.
	l 
			model: self;
			label: 'Load';
			help: 'Load notebook from .ipynb file';
			action: #actionLoad.
	s := builder pluggableButtonSpec new.
	s 
			model: self;
			label: 'Save';
			help: 'Save notebook in .ipynb file';
			action: #actionSave.
			
	panelSpec children addAll: {
		r.
		"builder pluggableSpacerSpec new."
		a.
		l.
		s.
	}.
	
	panelSpec frame: (LayoutFrame new
		leftFraction: 0 offset: 0;
		topFraction: 0 offset: 0;
		rightFraction: 1 offset: 0;
		bottomFraction: 0 offset: self buttonHeight).

	^panelSpec
]

{ #category : #toolbuilder }
PolyglotNotebook >> buildWith: builder [
	| windowSpec window |
	windowSpec := self buildWindowWith: builder specs: {
		(0@0corner: 0.6@1) -> [self buildCodePaneWith: builder].
		(0.6@0corner: 1@1) -> [self buildSidebar: builder].
	}.
	windowSpec
		label: 'Polyglot Notebook';
		extent: 1000@720.
	window := builder build: windowSpec.
	(window allMorphs select: [:m | m isKindOf: PluggableButtonMorphPlus ]) do: [:b | b borderWidth: 0].
	(window allMorphs detect: [:m |  m isKindOf: PluggableTreeMorph ]) borderWidth: 0; color: (Color fromString: '#F5F5F5').
	self showLanguageScopes ifFalse: [ self changed: #expandRootsRequested ].
	^ window.
]

{ #category : #model }
PolyglotNotebook >> buttonHeight [
	^ Preferences standardButtonFont height * 3
]

{ #category : #accessing }
PolyglotNotebook >> cellContainer [

	^ cellContainer ifNil: [ cellContainer := PNBCodeCellContainer for: self ]
]

{ #category : #accessing }
PolyglotNotebook >> cellContainer: anObject [

	cellContainer := anObject.
]

{ #category : #model }
PolyglotNotebook >> contents [
	^ ''
]

{ #category : #model }
PolyglotNotebook >> contentsSelection [
	^ 1 to: 0
]

{ #category : #model }
PolyglotNotebook >> currentSelection [

	^ currentSelection
]

{ #category : #model }
PolyglotNotebook >> currentSelection: anObject [

	self currentSelection == anObject ifTrue: [^ self].
	
	currentSelection := anObject.
	
	self changed: #currentSelection.
]

{ #category : #overrides }
PolyglotNotebook >> defaultWindowColor [
	^ Color white
]

{ #category : #actions }
PolyglotNotebook >> explorerKey: aChar from: view event: event [

	event anyModifierKeyPressed ifFalse: [^ false].

	self currentSelection ifNotNil: [ :obj |
		aChar == $i ifTrue: [obj value inspect. ^ true].
		aChar == $I ifTrue: [obj value explore. ^ true] ].

	^ false
]

{ #category : #actions }
PolyglotNotebook >> genericMenu: aMenu [ 
	^ aMenu
		add: 'inspect (i)'
			target: self currentSelection value
			selector: #inspect;
		add: 'explore (I)'
			target: self currentSelection value
			selector: #explore
]

{ #category : #model }
PolyglotNotebook >> getList [
	^ {'bindings' -> cellContainer bindings },
		(self showLanguageScopes
			ifTrue: [ Polyglot availableLanguages collect: [ :ea |
						ea getName asString -> (Polyglot primitiveGetScope: ea getId asSymbol) ] ]
			ifFalse: [ #() ])
]

{ #category : #actions }
PolyglotNotebook >> getMenu: shiftKeyState [ 
	^ (shiftKeyState not or: [Preferences noviceMode])
		ifTrue: [TextEditor yellowButtonMenu]
		ifFalse: [TextEditor shiftedYellowButtonMenu]
]

{ #category : #accessing }
PolyglotNotebook >> showLanguageScopes [
	^ showLanguageScopes ifNil: [ showLanguageScopes := false ]
]

{ #category : #actions }
PolyglotNotebook >> toggleShowLanguageScopes [
	showLanguageScopes := self showLanguageScopes not.
	self changed: #getList
]

{ #category : #actions }
PolyglotNotebook >> toggleShowLanguageScopesLabel [

	^self showLanguageScopes
		ifTrue: [ '<on> show language scopes' ]
		ifFalse: [ '<off> show language scopes' ]
]

{ #category : #toolbuilder }
PolyglotNotebook >> topConstantHeightFrame: height fromLeft: leftFraction width: rightFraction [
	^LayoutFrame new
		topFraction: 0 offset: 0;
		leftFraction: leftFraction offset: 0;
		rightFraction: (leftFraction + rightFraction) offset: 0;
		bottomFraction: 0 offset: height;
		yourself.
]

{ #category : #overrides }
PolyglotNotebook >> uniformWindowColor [
	^ Color white
]
