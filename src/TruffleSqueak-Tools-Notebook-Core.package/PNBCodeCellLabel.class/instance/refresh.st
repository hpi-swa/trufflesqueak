actions
refresh
	| cellColor |
	self label: (self labelFor: self codeCell languageID).
	cellColor := (PolyglotNotebook colorFor: self codeCell languageID).
	self color: cellColor.
	self codeCell borderColor: cellColor