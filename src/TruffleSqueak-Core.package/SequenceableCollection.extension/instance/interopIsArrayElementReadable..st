*TruffleSqueak-Core-arrays
interopIsArrayElementReadable: index
	^ 0 <= index and: [ index < self size ]