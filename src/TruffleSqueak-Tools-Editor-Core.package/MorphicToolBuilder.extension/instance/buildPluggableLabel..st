*TruffleSqueak-Tools-Editor-Core
buildPluggableLabel: aSpec
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