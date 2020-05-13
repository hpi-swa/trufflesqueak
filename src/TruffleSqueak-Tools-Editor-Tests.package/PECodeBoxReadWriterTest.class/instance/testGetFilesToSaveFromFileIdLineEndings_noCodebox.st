as yet unclassified
testGetFilesToSaveFromFileIdLineEndings_noCodebox

	| code codeFile result |
	codeFile := PEFileEntryTestFile new
		fullPath: '/tmp/code.js';
		yourself.
		
	code := '123
abc' withUnixLineEndings asText.


	result := PECodeBoxReadWriter getFilesToSaveFrom: code path: codeFile fullPath id: #js lineEndings: String lf.
	
	self assert: result size = 1.
	self assert: code asString equals: (result at: codeFile fullPath)