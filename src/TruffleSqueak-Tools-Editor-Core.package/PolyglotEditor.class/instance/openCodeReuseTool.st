editor functions
openCodeReuseTool

	ToolBuilder open: ((Smalltalk at: #CodeReuseBuilder) 
		newFor: self
		withCallback: [:instance :language :code | 
			(self currentLanguageInfo isNil or: [self currentLanguageInfo polyglotId = language])
				ifTrue: [self activeEditArea replaceSelectionWith: code asString]
				ifFalse: [ | codeBox |
					codeBox := PECodeBox new
						code: code;
						language: language;
						yourself.
					self insertCodeBox: codeBox]]
		withLabel: 'Insert code')