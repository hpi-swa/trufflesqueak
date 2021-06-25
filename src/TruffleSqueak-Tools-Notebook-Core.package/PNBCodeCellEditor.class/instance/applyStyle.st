actions
applyStyle
	RougeStyler default style: self contents language: self codeCell languageID.
	self paragraph composeAll.
	self editor recomputeSelection.
	self updateFromParagraph.
	self editor blinkParen