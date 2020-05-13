typing/selecting keys
forwardDelete: aKeyboardEvent
	"Delete forward over the next character.
	  Make Undo work on the whole type-in, not just the one char.
	wod 11/3/1998: If there was a selection use #zapSelectionWith: rather than #backspace: which was 'one off' in deleting the selection. Handling of things like undo or typeIn area were not fully considered."

	| startIndex stopIndex |
	morph readOnly ifTrue: [^ self].
	
	self hasSelection ifTrue: [
		"Create checkpoint in history."
		self replaceSelectionWith: self nullText.
		^ true].

	startIndex := self markIndex.
	startIndex > self text size ifTrue: [^ false].

	stopIndex := (self isLineEndingAt: startIndex)
		ifTrue: [startIndex + self lineEndings size - 1]
		ifFalse: [startIndex].

	"Forward delete next word"
	self flag: #consistency. "mt: We might want to implemented it like #backspace: and #backWord:."
	aKeyboardEvent shiftPressed
		ifTrue: [stopIndex := (self firstWordBoundaryAfter: stopIndex) - 1].

	self selectInvisiblyFrom: startIndex to: stopIndex.
	
	self isTypingIn ifTrue: [
		self history current type = #forwardDelete
			ifFalse: [self closeTypeIn]
			ifTrue: [
				"Append next characters that will be removed."
				self history current contentsBefore append: self selection.
				self history current intervalBefore in:  [:i |
					self history current intervalBefore: (i first to: i last + (stopIndex - startIndex + 1))]]].
	
	self openTypeInFor: #forwardDelete.
	self zapSelectionWith: self nullText.
	
	^ false