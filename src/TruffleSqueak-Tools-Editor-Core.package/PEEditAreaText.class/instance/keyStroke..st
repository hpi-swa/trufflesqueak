keyboard shortcuts
keyStroke: aKeyboardEvent

	| key shortcutMap |
	(aKeyboardEvent controlKeyPressed and: [aKeyboardEvent shiftPressed]) ifFalse: [^ false].
	key := aKeyboardEvent keyCharacter.
	shortcutMap := (Dictionary newFrom: {
		$C -> #generatePolyglotCodeBox.
		$E -> #generatePolyglotEvaluate.
		$X -> #generatePolyglotExport.
		$I -> #generatePolyglotImport.
		$S -> #searchForCode}).
	shortcutMap keysAndValuesDo: [:character :symbol |
		"The second condition is here to support Squeak on Windows and GraalSqueak (on all platforms). For some reason, the keyCharacter corresponds to the position in the alphabet when pressing shift and control on these platforms."
		(key = character or: [key asInteger = (character digitValue - 9)])
			ifTrue: [self triggerEvent: symbol. ^ true]].
	
	^ false