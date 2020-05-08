redirects
_redirect: identifier
	^ self _dispatch: identifier
		ifFail: [ self error: 'Foreign object does not understand `', identifier, '`' ]