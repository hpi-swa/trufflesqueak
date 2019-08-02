dispatching
_dispatch: identifier ifFail: failBlock
	^ self _dispatch: identifier arguments: #() ifFail: failBlock