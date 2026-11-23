testing
testMethodInstalledAfterFailedLookup
	"A send site that has cached a failed lookup must find a method that is installed into an existing method dictionary afterwards, once caches are flushed for its selector."
	| results |
	results := OrderedCollection new.
	[ 1 to: 3 do: [ :i |
		results add: ([ Object new tsInstalledAfterFailedLookup ]
			on: MessageNotUnderstood
			do: [ :e |
				Object methodDict at: #tsInstalledAfterFailedLookup put: (ProtoObject >> #isNil) copy.
				#tsInstalledAfterFailedLookup flushCache.
				e return: #notUnderstood ]) ] ]
		ensure: [
			Object methodDict removeKey: #tsInstalledAfterFailedLookup ifAbsent: [].
			#tsInstalledAfterFailedLookup flushCache ].
	self assert: #(#notUnderstood false false) equals: results asArray
