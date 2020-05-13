running code
runSelected

	self activeEditArea fileEntry ifNil: [UIManager inform: 'No file opened'. ^ self].
	self activeEditArea isCodeEntry ifFalse: [UIManager inform: 'Not a valid code file, cannot be run'. ^ self].
	self currentLanguageInfo polyglotId ifNil: [UIManager inform: 'No Polyglot language recognized'. ^ self].

	self activeEditArea fileEntry ifNotNil: [
		self polyglotEvaluateCurrentFile inspect]