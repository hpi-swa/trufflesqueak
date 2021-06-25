public-api
style: aText language: aLanguageID
	self style: aText language: aLanguageID
		ifFail: [ aLanguageID = Polyglot languageIdSmalltalk
			ifTrue: [ (stStyler ifNil: [ stStyler := SHTextStylerST80 new ]) styledTextFor: aText ]
			ifFalse: [ aText ] ]