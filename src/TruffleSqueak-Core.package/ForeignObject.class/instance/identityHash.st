overrides
identityHash
	^ (Interop hasIdentity: self)
		ifTrue: [ Interop identityHashCode: self ]
		ifFalse: [Interop hostIdentityHashCode: self ]