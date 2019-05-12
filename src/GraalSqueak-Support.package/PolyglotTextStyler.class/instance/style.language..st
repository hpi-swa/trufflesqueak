as yet unclassified
style: aText language: aLanguageID
	^ Polyglot supportsRuby
		ifTrue: [ | rubyString |
			rubyString := self rubyUnbox: aText asString withUnixLineEndings.
			((self formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
				asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml]
		ifFalse: [ aLanguageID = 'squeaksmalltalk'
			ifTrue: [ SHTextStylerST80 new styledTextFor: aText ]
			ifFalse: [ aText ] ]