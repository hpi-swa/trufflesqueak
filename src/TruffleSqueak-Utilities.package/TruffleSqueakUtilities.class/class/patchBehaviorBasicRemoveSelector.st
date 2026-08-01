helpers
patchBehaviorBasicRemoveSelector
	"See https://github.com/hpi-swa/trufflesqueak/issues/287"

	(Behavior>>#basicRemoveSelector:) timeStamp = 'ul 4/2/2016 20:45'
		ifTrue: [ Behavior compile: 'basicRemoveSelector: selector 
	"Assuming that the argument, selector (a Symbol), is a message selector 
	in my method dictionary, remove it and its method. Returns the old method
	if found, nil otherwise."

	| oldMethod |
	oldMethod := self methodDict at: selector ifAbsent: [^ nil].
	self methodDict removeKey: selector.

	"Now flush Squeak''s method cache for this selector"
	selector flushCache.

	"For backwards compatibility with other VMs (e.g., TruffleSqueak), also
	invalidate via old method. See commentary in CompiledCode >> #flushCache."
	oldMethod flushCache.
	
	^oldMethod'
			classified: 'patched' ]
		ifFalse: [ self notify: '#basicRemoveSelector: patch does no longer apply' ].