actions
evaluateExpression:	aText
	| code |
	self codeCell isMarkdown ifTrue: [ ^ nil ].
	code := aText asString.
	^ (code beginsWith: '%ggplot2')
		ifTrue: [	 | codeWithoutFirstLine |
				codeWithoutFirstLine := code lines allButFirst joinSeparatedBy: String cr.
				(PNBImageMorph for: (Polyglot eval: 'R' string: 
('library(ggplot2);
bindings <- import("bindings")', String cr,
codeWithoutFirstLine, String cr,
'file <- tempfile(fileext = ".png")
ggsave(file, device = "png", dpi = 72)
print(file)')) asString ) ]
		ifFalse: [ Polyglot eval: self codeCell languageID string: code withBindingsNamed: 'bindings' ]