Extension { #name : #Dictionary }

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopGetHashEntriesIterator [
	^ ReadStream on: self associations
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopGetHashKeysIterator [
	^ ReadStream on: self keys
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopGetHashSize [
	^ self size
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopGetHashValuesIterator [
	^ ReadStream on: self values
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Dictionary >> interopGetIterator [
	^ self interopGetHashEntriesIterator
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopHasHashEntries [
	^ true
]

{ #category : #'*TruffleSqueak-Core-iterator' }
Dictionary >> interopHasIterator [
	^ true
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopIsHashEntryExisting: key [
	^ self includesKey: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopIsHashEntryInsertable: key [
	^ (self interopIsHashEntryExisting: key) not
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopIsHashEntryModifiable: key [
	^ self interopIsHashEntryExisting: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopIsHashEntryReadable: key [
	^ self interopIsHashEntryExisting: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopIsHashEntryRemovable: key [
	^ self interopIsHashEntryExisting: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopReadHashValue: key [
	^ self at: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopReadHashValueOrDefault: key and: value [
	^ self at: key ifAbsent: value
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopRemoveHashEntry: key [
	^ self removeKey: key
]

{ #category : #'*TruffleSqueak-Core-hashes' }
Dictionary >> interopWriteHashEntry: key and: value [
	^ self at: key put: value
]
