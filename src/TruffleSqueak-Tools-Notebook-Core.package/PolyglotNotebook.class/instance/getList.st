model
getList
	^ {'bindings' -> cellContainer bindings },
		(self showLanguageScopes
			ifTrue: [ Polyglot availableLanguages collect: [ :ea |
						ea getName asString -> (Polyglot primitiveGetScope: ea getId asSymbol) ] ]
			ifFalse: [ #() ])