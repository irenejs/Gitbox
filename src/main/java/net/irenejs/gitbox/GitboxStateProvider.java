package net.irenejs.gitbox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;



public class GitboxStateProvider implements IStateProvider{
	
	private static Logger LOG = Logger.getLogger(GitboxStateProvider.class.getName());

	private State state = State.NOT_INITIALIZED;
	private Map<String, String> context = new HashMap<>();
	
	@Override
	public State getState() {
		return state;
	}

	@Override
	public Map<String, String> getContext() {
		return Collections.unmodifiableMap(context);
	}

	@Override
	public void update(State state, String... contextValues) {
		State oldState = this.state;
		this.state = state;
		for (int i = 0; i < contextValues.length; i++) {
			int keyIndex = i;
			int valueIndex = i+1;
			if (valueIndex <= contextValues.length)
			{	
				context.put(contextValues[keyIndex], contextValues[valueIndex]);
				i++;
			}
		}
		stateUpdated(oldState, state, context);
	}
	
	@Override
	public void updateIfNoError(State state, String... contextValues) {
		if (this.state!=State.ERROR)
		{
			update(state, contextValues);
		}
		
	}
	
	private void stateUpdated(State oldState, State newState, Map<String, String> updatedContext) {
		LOG.info("previousState="+oldState+" newState="+newState);
	}
	

}
