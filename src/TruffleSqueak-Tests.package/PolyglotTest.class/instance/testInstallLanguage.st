testing
testInstallLanguage
	Java primitiveIsHostLookupAllowed ifFalse: [ ^self ].
	Polyglot installLanguage: 'js' version: '23.1.0' path: Smalltalk imagePath