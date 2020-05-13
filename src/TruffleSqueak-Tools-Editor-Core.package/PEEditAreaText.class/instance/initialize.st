initialize-release
initialize

	super initialize.
	self
		code: '-- no file selected --';
		lineEndings: String lf;
		textStyler: PETextStyler new.
	lineEndingsSymbol := #lf