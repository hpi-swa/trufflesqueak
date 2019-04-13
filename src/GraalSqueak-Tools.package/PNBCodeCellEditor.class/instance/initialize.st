initialization
initialize
	super initialize.
	self
		setProperty: #defaultContents toValue: '' asText;
		borderColor: Color gray;
		hResizing: #spaceFill;
		margins: 2@2