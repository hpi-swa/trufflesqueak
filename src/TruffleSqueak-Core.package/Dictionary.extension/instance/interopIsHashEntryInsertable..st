*TruffleSqueak-Core-hashes
interopIsHashEntryInsertable: key
	^ (self interopIsHashEntryExisting: key) not