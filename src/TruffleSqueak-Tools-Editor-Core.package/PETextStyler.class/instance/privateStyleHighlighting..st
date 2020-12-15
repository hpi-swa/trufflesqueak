private
privateStyleHighlighting: aText

	| tokens polyglot |
	self languageInfo ifNil: [^ aText].
	polyglot := Smalltalk at: #Polyglot ifAbsent: [^ aText].
	polyglot primitiveIsPolyglotBindingsAccessAllowed ifFalse: [^ aText].
	
	polyglot export: 'polyglot-editor:code' value: aText asString.
	polyglot export: 'polyglot-editor:lexerName' value: self languageInfo rougeLexerName.
	
	aText 
		removeAttributesThat: [:attribute | attribute class = TextColor] 
		replaceAttributesThat: [:attribute | false] 
		by: [].
	tokens := ([polyglot eval: polyglot languageIdRuby string: self rubyHighlightCode] ifError: [^ aText]) asCollection.
	tokens do: [:ea | | each |
		each := ea asCollection. 
		aText 
			addAttribute: (self toTextColor: each third asString)
			from: each first 
			to: each second - 1].
	
	^ aText