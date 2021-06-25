public-api
style: aText language: aLanguageID
	| rubyString |
	rubyString := rubyUnbox value: aText asString withUnixLineEndings.
	aText runs: ((formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
		asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml runs