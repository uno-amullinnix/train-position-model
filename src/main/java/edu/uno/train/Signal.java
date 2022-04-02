package edu.uno.train;

public class Signal {
	/*
	 * signal state indicates *both* the signal indication and direction
	 * 0 = dark
	 * 1 = RED     (STOP)for trains moving MP increasing (e.g. the signal is facing train moving in the MP increasing direction (technically it is facing MP decreasing)
	 * 2 = YELLOW  (APPROACH) for trains moving MP increasing
	 * 3 = FLASHING_YELLOW (ADVANCE-APPROACH) for trains moving MP increasing
	 * 4 = GREEN   (CLEAR) for trains moving MP increasing
	 * -1 = RED    (STOP)for trains moving MP decreasing (e.g. the signal is facing trains moving in the MP decreasing direction (technically it is facing MP increasing)
	 * -2 = YELLOW (APPROACH) for trains moving MP decreasing
	 * -3 = FLASHING_YELLOW (ADVANCE-APPROACH) for trains moving MP decreasing
	 * -4 = GREEN  (CLEAR) for trains moving MP decreasing
	 */
	private int signalState;
	private float mpLocation;  //The location of this signal such that 101.25 => 1/4 of the distance between 101 and 102
}
