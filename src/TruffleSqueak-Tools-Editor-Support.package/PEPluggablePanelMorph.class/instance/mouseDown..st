event handling
mouseDown: evt

	onClickSelector ifNil: [^ super mouseDown: evt].
	onClickSelector isSymbol 
		ifTrue: [self model perform: onClickSelector]
		ifFalse: [onClickSelector value]