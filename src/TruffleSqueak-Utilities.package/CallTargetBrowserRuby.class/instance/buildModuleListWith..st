toolbuilder
buildModuleListWith: builder
	| listSpec |
	listSpec := builder pluggableListSpec new.
	listSpec 
		model: self;
		list: #moduleList; 
		getIndex: #moduleListIndex;
		setIndex: #moduleListIndex:.
	^ listSpec