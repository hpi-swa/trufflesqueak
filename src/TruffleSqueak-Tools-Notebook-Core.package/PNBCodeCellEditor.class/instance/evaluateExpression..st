actions
evaluateExpression:	aText
	| code bindings |
	self codeCell isMarkdown ifTrue: [ ^ nil ].
	code := aText asString.
	bindings := self codeCell cellContainer bindings.
	^ (code beginsWith: '%ggplot2')
		ifTrue: [ (PNBRPlotMorph
					extent: (self width - 10)@(self width)
					plotCode: (code allButFirst: 9))
					bindings: bindings;
					yourself ]
		ifFalse: [ Polyglot eval: self codeCell languageID string: code
					names: #('bindings') arguments: {bindings} ]