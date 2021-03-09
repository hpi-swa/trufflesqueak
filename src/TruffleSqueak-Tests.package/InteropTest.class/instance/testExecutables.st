testing
testExecutables
	| block |
	self deny: (Interop isExecutable: Smalltalk).
	
	block := [:x | x + 1].
	self assert: (Interop isExecutable: block).
	self assert: 42 equals: (Interop execute: block with: #(41)).
	self assert: (Interop hasExecutableName: block).
	self assert: '[closure] in InteropTest>>testExecutables' equals: (Interop getExecutableName: block)
