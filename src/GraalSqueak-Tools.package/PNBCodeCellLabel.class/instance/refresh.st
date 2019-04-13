actions
refresh
	| color |
	self label: (self labelFor: self codeCell languageID asLowercase).
	color := (PolyglotNotebook colorFor: self codeCell languageID).
	self color: color.
	self codeCell borderColor: color