helpers
callTargetFor: aMethodName of: aModuleName
	| vmObject |
	vmObject := (self methodFor: aMethodName of: aModuleName) vmObject.
	^ (vmObject respondsTo: #function) 
		ifTrue: [ (vmObject function respondsTo: #callTarget)
				ifTrue: [ vmObject function callTarget ]
				ifFalse: nil ]
		ifFalse: nil