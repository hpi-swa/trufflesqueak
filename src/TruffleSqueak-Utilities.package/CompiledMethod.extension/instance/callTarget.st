*TruffleSqueak-Utilities
callTarget
	^ self vmObject ifNotNil: [ :c | c getCallTargetOrNull ]