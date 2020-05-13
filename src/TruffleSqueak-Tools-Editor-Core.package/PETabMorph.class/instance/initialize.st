initialization
initialize

	| closeMorph |
	super initialize.
	
	closeMorph := SystemWindow closeBoxImage asMorph
		eventHandler: (EventHandler new
			on: #mouseDown send: #close to: self);
		yourself.
	
	self
		changeTableLayout;
		height: 25;
		vResizing: #rigid;
		hResizing: #shrinkWrap;
		layoutInset: 3;
		cellInset: 3;
		listDirection: #leftToRight;
		listCentering: #center;
		addMorphBack: StringMorph new;
		addMorphBack: closeMorph;
		eventHandler: (EventHandler new
			on: #mouseDown send: #click to: self);
		state: #unselected