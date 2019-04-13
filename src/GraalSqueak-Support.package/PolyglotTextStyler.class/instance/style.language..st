as yet unclassified
style: aText language: aLanguageID
	^ Polyglot supportsRuby
		ifTrue: [ ((self formatter format: (
			(self lexerFor: aLanguageID) lex: aText asString withUnixLineEndings))
				asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml]
		ifFalse: [ aLanguageID = 'squeaksmalltalk'
			ifTrue: [ SHTextStylerST80 new styledTextFor: aText ]
			ifFalse: [ aText ] ]