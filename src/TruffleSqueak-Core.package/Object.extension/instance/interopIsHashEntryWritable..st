*TruffleSqueak-Core-hashes
interopIsHashEntryWritable: key
	^ (self interopIsHashEntryModifiable: key) or: [ self interopIsHashEntryInsertable: key ]