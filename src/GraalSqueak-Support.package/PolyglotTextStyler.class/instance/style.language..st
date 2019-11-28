as yet unclassified
style: aText language: aLanguageID
	^ (Polyglot isPermitted: 'ruby')
		ifTrue: [ | rubyString |
			rubyString := self rubyUnbox: aText asString withUnixLineEndings.
			((self formatter format: ((self lexerFor: aLanguageID) lex: rubyString))
				asString copyReplaceAll: String lf with: '<br/>') asTextFromHtml]
		ifFalse: [ aLanguageID = Polyglot languageIdSmalltalk
			ifTrue: [ SHTextStylerST80 new styledTextFor: aText ]
			ifFalse: [ aText ] ]