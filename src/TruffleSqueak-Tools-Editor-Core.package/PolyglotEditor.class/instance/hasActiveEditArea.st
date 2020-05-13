panes
hasActiveEditArea

	^ self panes anySatisfy: [:each | each isVisibleEditAreaActive]