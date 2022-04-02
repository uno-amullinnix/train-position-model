package edu.uno.train;

import java.util.List;

/**
 * Represents all the devices/switches/trains in some milepost area
 * e.g. milepost number 101 => 101.00 - 101.99
 * @author Igen172
 *
 */
public class TrackMilepostArea {
	private int subdivisionId;   //The ID for the subdivision this milepost is in
	private String trackName;    //The name of the track
	private int milepostNumber;  //The Milepost whole number 
	
	private List<Switch> switchList;
	private List<Signal> signalList;
	private List<Train>  trainList;
	
}
