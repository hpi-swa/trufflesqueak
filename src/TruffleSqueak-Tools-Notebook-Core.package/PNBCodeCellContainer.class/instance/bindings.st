accessing
bindings

	^ bindings ifNil: [ bindings := PNBKeyValueStore new ]