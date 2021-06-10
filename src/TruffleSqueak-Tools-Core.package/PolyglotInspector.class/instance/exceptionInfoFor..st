helpers
exceptionInfoFor: anInteropException
	^ 'Message:	{1}
Cause:		{2}
Type:		{3}

Stack trace:
- {4}' format: {
	(Interop hasExceptionMessage: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ Interop getExceptionMessage: anInteropException ].
	(Interop hasExceptionCause: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ Interop getExceptionCause: anInteropException ].
	Interop getExceptionType: anInteropException.
	(Interop hasExceptionStackTrace: anInteropException)
		ifFalse: [ 'n/a' ]
		ifTrue: [ ((Interop getExceptionStackTrace: anInteropException) asCollection
					collect: [ :ea | ((Interop hasExecutableName: ea)
									ifTrue: [ (Interop getExecutableName: ea) ]
									ifFalse: [ ea ]) asString ]) joinSeparatedBy: String cr, '- ' ] }