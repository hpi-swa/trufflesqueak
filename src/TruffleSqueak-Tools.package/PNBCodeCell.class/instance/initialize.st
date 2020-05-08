initialize-release
initialize
	super initialize.
	self
		color: Color white;
		layoutPolicy: TableLayout new;
	  	listDirection: #topToBottom;
	  	hResizing: #spaceFill;
	  	vResizing: #shrinkWrap;
		borderWidth: 2;
	  	layoutInset: 0.
	self addMorph: self editor.
	self addMorph: self label.