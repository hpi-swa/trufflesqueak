helpers
addCodeCellFor: cell
	| codeCell |
	codeCell := (PNBCodeCell for: self)
		languageID: (cell metadata polyjus_language ifNil: [nil] ifNotNil: [:a | a asSymbol]) ;
		outputs: cell outputs;
		yourself.
	codeCell editor contents: cell source join.
	codeCell label refresh.
	self addMorphBack: codeCell.
	self changed: #submorphs