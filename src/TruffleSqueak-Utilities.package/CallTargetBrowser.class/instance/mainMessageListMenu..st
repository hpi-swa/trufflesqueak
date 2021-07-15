message functions
mainMessageListMenu: aMenu 
	^ (super mainMessageListMenu: aMenu)
		add: 'inspect call target' action: #inspectCallTarget;
		addLine;
		add: 'sort alphabetically' action: #sortAlphabetically;
		add: 'sort by call and loop count (default)' action: #sortByCallAndLoopCount;
		add: 'sort by call count' action: #sortByCallCount;
		add: 'sort by known call site count' action: #sortByKnownCallSiteCount;
		add: 'sort by non-trivial node count' action: #sortByNonTrivialNodeCount;
		addLine;
		add: 'toggle coverage based on Truffle ASTs' action: #toggleCoverage;
		addLine;
		yourself