actions
run
	| result |
	self isMarkdown ifTrue: [ ^ self ].
	result := self editor evaluate.
	executionCount := self cellContainer incrementExecutionCounter.
	result isNull ifFalse: [self output displayResult: result.
		self cellContainer changed: #submorphs].
		self cellContainer notebook changed: #getList.
		self cellContainer notebook changed: #expandRootsRequested.