initialization
initializeTextMorph

	| padding |
	super initializeTextMorph.
	padding := 3.
	textMorph
		margins: ((padding)@0 corner: padding@0);
		setOnKeyStrokeSelector: onKeyStrokeSelector