polyglot-api
isAOT
	^ Java primitiveIsHostLookupAllowed and: [ (Java type: 'com.oracle.truffle.api.TruffleOptions') AOT ]