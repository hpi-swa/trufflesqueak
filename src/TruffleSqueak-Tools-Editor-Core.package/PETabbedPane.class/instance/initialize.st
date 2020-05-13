initialization
initialize

	super initialize.
	
	editArea := nil.
	editAreas := OrderedCollection new.
	self
		changeTableLayout;
		listDirection: #topToBottom;
		hResizing: #spaceFill;
		vResizing: #spaceFill;
		color: Color transparent;
		addMorphBack: (tabs := PETabsMorph new)