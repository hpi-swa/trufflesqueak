public-api
style: aText language: aLanguageID
	| rubyString newText |
	rubyString := rubyUnbox value: aText asString withUnixLineEndings.
	newText := ((formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
		asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml.
	aText setString: newText string setRuns: newText runs