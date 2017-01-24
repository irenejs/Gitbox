package net.irenejs.gitbox;

import java.util.Map;

public interface IStateProvider {
	
	public static enum State {
		NOT_INITIALIZED,
		INITIALIZING,
		READY,
		SYNCHRONIZING,
		ERROR
	}
	
	public State getState();
	public Map<String, String> getContext();
	
	public void update(State oldState, String... contextValues);
	public void updateIfNoError(State oldState, String... contextValues);

}
