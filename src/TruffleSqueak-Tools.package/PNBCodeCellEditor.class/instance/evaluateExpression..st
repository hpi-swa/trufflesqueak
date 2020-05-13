actions
evaluateExpression:	aText
	| code |
	self codeCell isMarkdown ifTrue: [ ^ nil ].
	code := aText asString.
	^ (code beginsWith: '%ggplot2')
		ifTrue: [ PNBRPlotMorph
					extent: (self width - 10)@(self width)
					plotCode: (code allButFirst: 9) ]
		ifFalse: [ Polyglot eval: self codeCell languageID string: code withBindingsNamed: 'bindings' ]