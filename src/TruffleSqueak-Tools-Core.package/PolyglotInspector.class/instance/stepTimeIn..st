updating - steps
stepTimeIn: aWindow
	^ (selectionUpdateTime ifNil: [0]) * 10 max: 4000