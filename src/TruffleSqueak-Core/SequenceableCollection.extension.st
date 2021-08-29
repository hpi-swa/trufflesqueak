Extension { #name : #SequenceableCollection }

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopGetArraySize [
	^ self size
]

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopHasArrayElements [
	^ true
]

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopIsArrayElementModifiable: index [
	^ self interopIsArrayElementReadable: index
]

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopIsArrayElementReadable: index [
	^ 0 <= index and: [ index < self size ]
]

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopReadArrayElement: index [
	^ self at: index + 1
]

{ #category : #'*TruffleSqueak-Core-arrays' }
SequenceableCollection >> interopWriteArrayElement: index and: value [
	^ self at: index + 1 put: value
]
