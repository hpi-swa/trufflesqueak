helpers
callTargetFor: aMethodName of: aModuleName
	^ (self methodFor: aMethodName of: aModuleName)
		vmObject method callTarget