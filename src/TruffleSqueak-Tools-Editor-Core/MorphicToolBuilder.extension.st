Extension { #name : #MorphicToolBuilder }

{ #category : #'*TruffleSqueak-Tools-Editor-Core' }
MorphicToolBuilder >> buildFileExplorer: aPCFileExplorerSpec [

	| widget aSpec |
	aSpec := PluggableTreeSpec new
		hasChildren: #hasChildren:;
		getChildren: #getChildren:;
		roots: #rootDirectories;
		setSelected: #selectedEntry:;
		getSelected: #selectedEntry;
		label: #nameOf:;
		menu: #menu:;
		frame: aPCFileExplorerSpec frame;
		autoDeselect: false;
		yourself.
	
	widget := PEFileExplorerMorph new.
	widget realModel: aPCFileExplorerSpec model.
	widget getRealRootsSelector: aPCFileExplorerSpec getRootDirectories.
	widget setRealSelectedFileEntrySelector: aPCFileExplorerSpec setSelectedFileEntry.
	
	self register: widget id: aSpec name.
	widget model: PEFileExplorerModel new.
	widget getSelectedPathSelector: aSpec getSelectedPath.
	widget setSelectedPathSelector: aSpec setSelectedPath.
	widget setSelectedSelector: aSpec setSelected.
	widget getSelectedSelector: aSpec getSelected.
	widget setSelectedParentSelector: aSpec setSelectedParent.
	widget getChildrenSelector: aSpec getChildren.
	widget hasChildrenSelector: aSpec hasChildren.
	widget getLabelSelector: aSpec label.
	widget getIconSelector: aSpec icon.
	widget getHelpSelector: aSpec help.
	widget getMenuSelector: aSpec menu.
	widget keystrokeActionSelector: aSpec keyPress.
	widget nodeClass: aSpec nodeClass.
	widget getRootsSelector: aSpec roots.
	widget autoDeselect: aSpec autoDeselect.
	widget doubleClickSelector: aSpec doubleClick.
	
	widget dropItemSelector: aSpec dropItem.
	widget wantsDropSelector: aSpec dropAccept.
	widget dragItemSelector: aSpec dragItem.
	widget dragStartedSelector: aSpec dragStarted.
	
	widget columns: aSpec columns.
	
	"Override default scroll bar policies if needed. Widget will use preference values otherwise."
	aSpec hScrollBarPolicy ifNotNil: [:policy |
		policy caseOf: {
			[#always] -> [widget alwaysShowHScrollBar].
			[#never] -> [widget hideHScrollBarIndefinitely].
			[#whenNeeded] -> [widget showHScrollBarOnlyWhenNeeded]. } ].
	aSpec vScrollBarPolicy ifNotNil: [:policy |
		policy caseOf: {
			[#always] -> [widget alwaysShowVScrollBar].
			[#never] -> [widget hideVScrollBarIndefinitely].
			[#whenNeeded] -> [widget showVScrollBarOnlyWhenNeeded]. } ].	
	
	self setFrame: aSpec frame in: widget.
	self setLayoutHintsFor: widget spec: aSpec.
	
	parent ifNotNil:[self add: widget to: parent].
"	panes ifNotNil:[
		aSpec roots ifNotNil:[panes add: aSpec roots].
	].	"
	^widget
]

{ #category : #'*TruffleSqueak-Tools-Editor-Core' }
MorphicToolBuilder >> buildPCPluggableCode: aSpec [
	| widget |
	"Largely based on buildPluggableText:"
	widget := PEPluggableCodeMorph basicNew.
	aSpec onKeyStroke ifNotNil: [widget setOnKeyStrokeSelector: aSpec onKeyStroke].
	widget initialize.
	
	aSpec stylerClass ifNotNil: [:c | widget styler: (c new view: widget)].
	aSpec styler ifNotNil: [:styler | widget styler: (styler view: widget)].
	
	aSpec onFocus ifNotNil: [widget setOnFocusSelector: aSpec onFocus].
	
	widget setLineEndingsSelector: aSpec setLineEndings.
	
	widget
		on: aSpec model
		text: aSpec getText 
		accept: aSpec setText
		readSelection: aSpec selection 
		menu: aSpec menu.
	aSpec font ifNotNil: [:f | widget font: f].
	
	widget setSelectionSelector: aSpec setSelection.
	widget setEditTextSelector: aSpec setEditText.
	
	widget readOnly: aSpec readOnly.
	widget editTextSelector: aSpec editText.
	widget wantsFrameAdornments: aSpec indicateUnacceptedChanges.

	widget askBeforeDiscardingEdits: aSpec askBeforeDiscardingEdits.
	widget setProperty: #alwaysAccept toValue: aSpec askBeforeDiscardingEdits not.

	aSpec softLineWrap ifNotNil: [:b | widget wrapFlag: b].

	widget isAutoFit
		ifTrue: [widget hideHScrollBarIndefinitely]
		ifFalse: [widget showHScrollBarOnlyWhenNeeded].
	
	self register: widget id: aSpec name.
	widget getColorSelector: aSpec color.
	widget getTextColorSelector: aSpec textColor.
	self buildHelpFor: widget spec: aSpec.
	 
	self setFrame: aSpec frame in: widget.
	self setLayoutHintsFor: widget spec: aSpec.
	
	aSpec autoHeight ifTrue: [
		widget autoHeight: true.
		widget vResizing: #shrinkWrap.
		"widget vScrollBarPolicy: #never"].
	
	parent ifNotNil:[self add: widget to: parent].
	
	^widget
]

{ #category : #'*TruffleSqueak-Tools-Editor-Core' }
MorphicToolBuilder >> buildPCPluggablePanel: aSpec [

	"Largely based on >>buildPluggablePanel:"
	| widget |
	widget := PEPluggablePanelMorph new.
	self register: widget id: aSpec name.

	widget model: aSpec model.

	"Set child dependent layout properties."
	widget wantsPaneSplitters: (aSpec wantsResizeHandles ifNil: [false]).
	self setLayoutHintsFor: widget spec: aSpec.
	widget layoutInset: (aSpec padding ifNil: [0]).
	widget cellInset: (aSpec spacing ifNil: [
		widget wantsPaneSplitters
			ifTrue: [ProportionalSplitterMorph gripThickness]
			ifFalse: [2]]).

	"Now create the children."
	aSpec children isSymbol
		ifTrue: [
			widget getChildrenSelector: aSpec children.
			widget update: aSpec children]
		ifFalse: [
			self buildAll: aSpec children in: widget].

	self setFrame: aSpec frame in: widget.
	self setLayout: aSpec layout in: widget.
	
	parent ifNotNil:[self add: widget to: parent].

	widget borderWidth: 0.
	self buildHelpFor: widget spec: aSpec. 
	
	(aSpec color isNil or: [aSpec color isSymbol]) 
		ifFalse: [widget color: aSpec color]
		ifTrue: [widget color: Color transparent].
	aSpec color isSymbol ifTrue: [
		widget color: (aSpec model perform: aSpec color).
		widget getColorSelector: aSpec color].
	
	widget onClickSelector: aSpec onClick.
	
	^ widget
]

{ #category : #'*TruffleSqueak-Tools-Editor-Core' }
MorphicToolBuilder >> buildPluggableLabel: aSpec [
	| widget label |
	label := aSpec label.
	widget := PEPluggableStringMorph new.
	self register: widget id: aSpec name.
	
	widget model: aSpec model.
	
	self setFrame: aSpec frame in: widget.
	"self setLayout: aSpec layout in: widget."
	
	(label isSymbol or: [label = nil]) ifFalse: [widget contents: label].
	label isSymbol ifTrue: [
		widget contents: (aSpec model perform: label).
		widget getLabelSelector: label].
	
	parent ifNotNil:[self add: widget to: parent].
	self buildHelpFor: widget spec: aSpec. 
	
	^widget
]
