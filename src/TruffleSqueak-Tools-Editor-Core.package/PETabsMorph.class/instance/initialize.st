initialization
initialize

	super initialize.
	self 
		changeTableLayout;
		hResizing: #spaceFill;
		vResizing: #shrinkWrap;
		color: Color transparent.
	
	tabHolder := Morph new
		color: Color transparent;
		changeTableLayout;
		listDirection: #leftToRight;
		hResizing: #shrinkWrap;
		vResizing: #shrinkWrap;
		cellInset: 3;
		yourself.
	scrollPane := ScrollPane new.
	scrollPane
		color: Color transparent;
		hResizing: #spaceFill;
		height: 25 + scrollPane scrollBarThickness;
		vScrollBarPolicy: #never;
		hScrollBarPolicy: #always.
	scrollPane scroller addMorphBack: tabHolder.
	self addMorph: scrollPane