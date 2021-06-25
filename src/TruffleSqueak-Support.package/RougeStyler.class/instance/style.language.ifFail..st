public-api
style: aText language: aLanguageID ifFail: aBlock
	(Polyglot isPermitted: 'ruby')
		ifTrue: [ | rubyString |
			self ensureInitialized.
			rubyString := self rubyUnbox: aText asString withUnixLineEndings.
			aText runs: ((self formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
				asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml runs ]
		ifFalse: aBlock