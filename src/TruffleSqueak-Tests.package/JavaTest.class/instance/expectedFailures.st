testing
expectedFailures
	^ Java primitiveIsHostLookupAllowed
		ifTrue: [
			"testJavaZoneId should be fixed in 21.0.0
			 see https://github.com/oracle/graal/issues/2978"
			#(#testJavaZoneId) ]
		ifFalse: [ self class allTestSelectors ]