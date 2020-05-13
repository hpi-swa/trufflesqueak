typing/selecting keys
cursorLeft: aKeyboardEvent

	self insertAndCloseTypeIn.
	self
		moveCursor:[:position | ((self isLineEndingBefore: position)
			ifTrue: [position - self lineEndings size]
			ifFalse: [position - 1]) max: 1]
		forward: false
		event: aKeyboardEvent
		specialBlock:[:position | self previousWord: position].
	^ true