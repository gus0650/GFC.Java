package gfc.util;


public final class StateMachine {

	private State current_state;
	
	public void toState(State s) {
		//no state yet at the first call
		if (current_state != null) current_state.leaveState();

		current_state = s;
		s.enterState();
	}
	
	public boolean isState(State s) {
		return( current_state == s );
	}
}
