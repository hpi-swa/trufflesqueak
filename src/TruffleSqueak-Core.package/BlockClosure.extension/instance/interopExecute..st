*TruffleSqueak-Core-executable
interopExecute: arguments
	^ (self numArgs == arguments size)
		ifTrue: [ self valueWithArguments: arguments ]
		ifFalse: [ "TODO: throw ArityException" nil ]