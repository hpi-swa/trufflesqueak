accessing
replaceSelectionWith: aText

	self code: (self code asText
		copyReplaceFrom: self selection start 
		to: self selection stop 
		with: aText).
	self selection: (self selection start to: (self selection stop + aText size))