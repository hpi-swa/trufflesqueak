helpers
labelFor: aLanguageID
	| languageName result |
	languageName := self codeCell isMarkdown
		ifTrue: [ PolyglotNotebook markdownName ]
		ifFalse: [(Polyglot languageNameFor: self codeCell languageID) ].
	result := Morph new
		color: Color transparent;
		layoutPolicy: TableLayout new;
		cellInset: 4;
		listDirection: #leftToRight;
	  hResizing: #shrinkWrap;
	  vResizing: #shrinkWrap.
	result addMorph: languageName asMorph.
	result addMorph: (Polyglot iconFor: aLanguageID) asMorph.
	^ result