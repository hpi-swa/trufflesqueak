public
state: aSymbol

	self assert: (#(unselected selected active) includes: aSymbol).
	state := aSymbol.
	state caseOf: {
		[#unselected] -> [self color: (Color gray: 0.91). self titleColor: Color black].
		[#selected] -> [self color: Color white. self titleColor: Color black].
		[#active] -> [self color: Color white. self titleColor: Color red]}