toolbuilder
buildMethodListWith: builder
	| listSpec |
	listSpec := builder pluggableListSpec new.
	listSpec 
		model: self;
		list: #methodList; 
		getIndex: #methodListIndex; 
		setIndex: #methodListIndex:; 
		icon: #methodIconAt:;
		menu: #methodListMenu:.
	^ listSpec
