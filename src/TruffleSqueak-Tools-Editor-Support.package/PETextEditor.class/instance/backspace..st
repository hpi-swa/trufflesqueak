typing/selecting keys
backspace: aKeyboardEvent 
	"Backspace over the last character."

	| startIndex charactersToRemove |
	aKeyboardEvent shiftPressed ifTrue: [^ self backWord: aKeyboardEvent].
	startIndex := self markIndex +
				(self hasCaret ifTrue: [0] ifFalse: [1]).
	
	(self isLineEndingBefore: startIndex) 
		ifTrue: [charactersToRemove := self lineEndings size]
		ifFalse: [charactersToRemove := 1].
	startIndex := 1 max: startIndex - charactersToRemove.

	^ self backTo: startIndex