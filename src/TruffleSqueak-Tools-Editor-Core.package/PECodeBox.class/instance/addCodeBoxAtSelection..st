building
addCodeBoxAtSelection: aPCCodeBox

	| codeBoxText |
	codeBoxText := 
		self innerLanguageInfo defaultLineEndings asText, 
		(Text
			string: (String value: 1) 
			attribute: (TextAnchor new anchoredMorph: (ToolBuilder build: aPCCodeBox))), 
		self innerLanguageInfo defaultLineEndings.
	self replaceSelectionWith: codeBoxText.
	self triggerEvent: #codeBoxAdded with: aPCCodeBox