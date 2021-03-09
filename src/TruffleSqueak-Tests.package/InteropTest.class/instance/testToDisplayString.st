testing
testToDisplayString
	{nil. true. false. #(1 3). #[54]. 'foo'. Object new. Smalltalk} do: [ :ea |
		self assert: ea printString equals: (Interop asString: (Interop toDisplayString: ea))]
