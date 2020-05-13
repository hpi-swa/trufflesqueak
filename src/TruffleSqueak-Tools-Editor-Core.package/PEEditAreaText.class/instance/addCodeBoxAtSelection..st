code actions
addCodeBoxAtSelection: aPCCodeBox

	| codeBoxText |
	codeBoxText := 
		self lineEndings asText,
		(Text
			string: (String value: 1) 
			attribute: (TextAnchor new anchoredMorph: (ToolBuilder build: aPCCodeBox))), 
		self lineEndings.
	self replaceSelectionWith: codeBoxText.
	self setupCodeBoxEventsFor: aPCCodeBox