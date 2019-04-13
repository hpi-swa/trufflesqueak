initialization
initialize
	super initialize.
	self layoutPolicy: ProportionalLayout new;
		hResizing: #spaceFill;
		vResizing: #rigid;
		borderWidth: 4;
		borderColor: (Color fromString: '#F5F5F5')