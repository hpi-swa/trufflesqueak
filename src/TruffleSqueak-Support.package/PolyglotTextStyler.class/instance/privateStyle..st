private
privateStyle: aText
	| languageId |
	languageId := view model languageInfo getId.
	(Polyglot isPermitted: 'ruby')
		ifTrue: [ | rubyString |
			rubyString := self rubyUnbox: aText asString withUnixLineEndings.
			aText runs: ((self formatter format: ((self lexerFor: languageId) lex: rubyString))
				asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml runs ]
		ifFalse: [ 
			languageId = Polyglot languageIdSmalltalk
				ifTrue: [super privateStyle: aText ] ]