helpers
callTargetFor: aMethodName of: aModuleName
	^ (TruffleSqueakUtilities primitiveGetVMObject: (
			self methodFor: aMethodName of: aModuleName))
				method callTarget