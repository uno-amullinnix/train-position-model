package edu.uno.train;

//represents a switch on a track
public class Switch {
	/*
	 * The switch State indicates *both* the switch direction state
	 * 0 == UNKNOWN/Switching (the state is changing or not known)
	 * 1 == facing milepost increasing and set to remain on this train
	 * -1 == facing milepost decreasing and set to remain on this train
	 * 2 == facing milepost increasing, but switched to leave this track
	 * -2 == facing milepost decreasing, and switched to leave this track
	 */
	private int switchState;
	private float mpLocation;  //The location of this switch such that 101.25 => 1/4 of the distance between 101 and 102

}
