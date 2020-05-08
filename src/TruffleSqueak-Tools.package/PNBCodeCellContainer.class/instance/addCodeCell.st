helpers
addCodeCell
	| codeCell  |
	codeCell := PNBCodeCell for: self.
	self addMorphBack: codeCell.
	self changed: #submorphs