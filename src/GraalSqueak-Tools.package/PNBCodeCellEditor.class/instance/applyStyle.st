actions
applyStyle
	self contents runs: (PolyglotTextStyler default style: self contents language: self codeCell languageID) runs.
	self paragraph composeAll.
	self editor recomputeSelection.
	self updateFromParagraph.
	self editor blinkParen