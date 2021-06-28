actions
run
	| result |
	self isMarkdown ifTrue: [ ^ self ].
	result := self editor evaluate.
	executionCount := self cellContainer incrementExecutionCounter.
	result ifNotNil: [self output displayResult: result.
		self cellContainer changed: #submorphs].
		self cellContainer notebook changed: #getList.
		self cellContainer notebook showLanguageScopes ifFalse: [
			self cellContainer notebook changed: #expandRootsRequested ]