code generation
insertCodeBox: aPCCodeBox

	self activeEditArea addCodeBoxAtSelection: aPCCodeBox.
	self activeEditArea ensurePolyglotIsImported