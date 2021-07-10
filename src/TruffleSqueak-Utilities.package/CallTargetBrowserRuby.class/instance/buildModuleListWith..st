toolbuilder
buildModuleListWith: builder
	| listSpec |
	listSpec := builder pluggableListSpec new.
	listSpec 
		model: self;
		list: #moduleAndClassList; 
		getIndex: #moduleAndClassListIndex;
		setIndex: #moduleAndClassListIndex:.
	^ listSpec