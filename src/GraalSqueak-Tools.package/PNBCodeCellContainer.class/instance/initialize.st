initialization
initialize
	super initialize.
	self layoutPolicy: TableLayout new;
	  listDirection: #topToBottom;
	  hResizing: #spaceFill;
	  vResizing: #shrinkWrap;
        cellInset: 4;
	  layoutInset: 0.
	  "rubberBandCells: true".
	self addCodeCell
