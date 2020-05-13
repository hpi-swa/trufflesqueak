initialization
initialize
	super initialize.
	self 
		borderWidth: 0;
	  	hResizing: #spaceFill;
		height: TextStyle defaultFont height * 1.8;
		target: self