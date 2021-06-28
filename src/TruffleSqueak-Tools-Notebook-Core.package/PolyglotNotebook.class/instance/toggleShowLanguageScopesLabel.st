actions
toggleShowLanguageScopesLabel

	^self showLanguageScopes
		ifTrue: [ '<on> show language scopes' ]
		ifFalse: [ '<off> show language scopes' ]