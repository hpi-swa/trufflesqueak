*TruffleSqueak-Tools-Editor-Core
buildPCPluggablePanel: aSpec

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