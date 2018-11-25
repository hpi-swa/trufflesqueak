overrides
selection
	self isForeign ifFalse: [^ super selection].
	selectionIndex = 0 ifTrue: [^ ''].
	selectionIndex = 1 ifTrue: [^ object].
	selectionIndex = 2 ifTrue: [^ object longPrintStringLimitedTo: 20000].
	^ object instVarAt: selectionIndex - 2