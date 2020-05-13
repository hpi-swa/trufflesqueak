event handling
handlesMouseDown: evt

	onClickSelector ifNotNil: [^ true].
	^ super handlesMouseDown: evt