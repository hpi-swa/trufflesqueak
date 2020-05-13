event handling
keyboardFocusChange: aBoolean
	super keyboardFocusChange: aBoolean.
	(aBoolean and: [onFocusSelector notNil]) ifTrue: [
		onFocusSelector isSymbol
			ifTrue: [self model perform: onFocusSelector]
			ifFalse: [onFocusSelector value]]