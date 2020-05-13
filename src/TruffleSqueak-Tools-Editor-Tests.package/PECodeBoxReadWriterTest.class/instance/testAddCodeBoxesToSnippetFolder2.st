as yet unclassified
testAddCodeBoxesToSnippetFolder2

	| result code attribute |
	
	"Test that adding code boxes works if there is no referenced code box and the snippet folder is nil"
	attribute := TextColor new.
	code := '123 abc 123 abc 123 abc' asText.
	code addAttribute: attribute from: 1 to: 10.
	result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
	self assert: code equals: result first.
	self assert: result second isEmpty.
	"Verify that our text attribute is still there."
	self assert: attribute equals: (result first attributesAt: 1) first.
	
	code := '123 123 123'.
	result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
	self assert: code asText equals: result first.
	self assert: result second isEmpty.
	
	"Test error message when the amount of begin and end markers for code boxes isn't the same"
	code := '
CODE BOX BEGIN: 

CODE BOX END

CODE BOX BEGIN: 
'.
	PEMockUIManager useDuring: [:uiManager | 
		result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
		self assert: uiManager hasInformed.
		self assert: code asText equals: result first.
		self assert: result second isEmpty.].
	
	"Test error message when using multiple different line endings"
	code := '
CODE BOX BEGIN: 

CODE BOX END', String lf, 'def', String cr.
	PEMockUIManager useDuring: [:uiManager | 
		result := PECodeBoxReadWriter addCodeBoxesTo: code file: nil.
		self assert: uiManager hasInformed.
		self assert: code asText equals: result first.
		self assert: result second isEmpty.].