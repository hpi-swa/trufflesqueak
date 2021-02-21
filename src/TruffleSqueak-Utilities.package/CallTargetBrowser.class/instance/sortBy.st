accessing
sortBy

	^ sortBy ifNil: [ sortBy := #callAndLoopCount ]