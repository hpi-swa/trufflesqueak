event handling
yellowButtonActivity: shiftState
	MenuMorph new
		add: 'export SVG...' target: self selector: #exportSVG;
		add: 'copy...' target: [ self copy openInHand ] selector: #value;
		add: 'clone form...' target: [ self form asMorph openInHand ] selector: #value;
		add: 'inspect...' target: self selector: #inspect;
		popUpInWorld