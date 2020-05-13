as yet unclassified
testGetJsonForBox

	| codeBox |
	codeBox := PECodeBox new
		boxName: 'name';
		exportVariables: {'a'. 'b'};
		importVariables: {'c'. 'd'};
		language: #js.
		
	self 
		assert: '{"boxName":"name","exportVariables":["a","b"],"importVariables":["c","d"],"language":"js"}' 
		equals: (PECodeBoxReadWriter getJsonForBox: codeBox)