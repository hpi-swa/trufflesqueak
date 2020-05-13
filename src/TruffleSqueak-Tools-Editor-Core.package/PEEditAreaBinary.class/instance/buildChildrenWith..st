private
buildChildrenWith: aBuilder

	^ {PEPluggableCodeSpec new
		model: self;
		getText: #data;
		onFocus: #triggerFocusEvent;
		frame: (0@0 extent: 1@1);
		yourself}