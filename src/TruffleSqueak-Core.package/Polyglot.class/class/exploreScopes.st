experimental
exploreScopes
	(Polyglot availableLanguages collect: [ :ea |
		(Polyglot primitiveGetScope: ea getId asSymbol) ] )
			exploreWithLabel: 'GraalVM Language Scopes'