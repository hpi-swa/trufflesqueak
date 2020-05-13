initialization
initialize

	super initialize.
	
	self
		textStyler: PETextStyler new;
		language: nil;
		boxName: '';
		code: '';
		importVariables: {};
		exportVariables: {}