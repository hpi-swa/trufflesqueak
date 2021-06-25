initialization
initializeTextMorph

	| padding |
	super initializeTextMorph.
	padding := 3.
	textMorph
		hResizing: #spaceFill;
		margins: ((padding)@0 corner: padding@0);
		setOnKeyStrokeSelector: onKeyStrokeSelector