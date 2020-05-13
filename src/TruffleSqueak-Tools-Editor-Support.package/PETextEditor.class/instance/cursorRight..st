typing/selecting keys
cursorRight: aKeyboardEvent

	self insertAndCloseTypeIn.
	self
		moveCursor:[:position | (self isLineEndingAt: position)
			ifTrue: [position + self lineEndings size]
			ifFalse: [position + 1]]
		forward: true
		event: aKeyboardEvent
		specialBlock:[:position | self nextWord: position].
	^ true