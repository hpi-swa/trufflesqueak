Extension { #name : #Object }

{ #category : #'*TruffleSqueak-Utilities' }
Object >> vmObject [
	^ TruffleSqueakUtilities primitiveGetVMObject: self
]

{ #category : #'*TruffleSqueak-Utilities' }
Object >> vmObjectInspect [
	self vmObject inspect
]
