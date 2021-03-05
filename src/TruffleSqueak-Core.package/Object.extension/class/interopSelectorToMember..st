*TruffleSqueak-Core
interopSelectorToMember: aSelector
	| member |
	member := aSelector asString.
	member last = $: ifTrue: [ member := member allButLast copyReplaceAll: ':' with: '_' ].
	^ member