package edu.uno.train;

public class Train {
    /*
     * The Alphabetic identifier of up to 7 positions that represents this Train.
     * <br> UP train symbols USUALLY break down into - <br> TOODD <br> ^^ ^ <br> ||
     * |_ Destination Code <br> ||___ Origin Code <br> |____ Train Type (Category)
     * <br> For a coal train going from the Newark Power Plant to the Antelope Mine,
     * the symbol would be built as follows- <br> CNWAT - where C is coal, NW is
     * Newark Power Plant, and AT is the Antelope Mine.
     */
    private String trainSymbol;

    /*
     * 1 = UP 2 = non-UP
     */
    private int trainRailroad;

    /*
     * The maximum train speed in MPH (from Loco Segment 2032). If there is no
     * equipment speed restriction associated with the train then this value is
     * populated with zero.
     */
    private int maximumTrainSpeed;

    private int loadedCarCount;

    private int emptyCarCount;

    /*
     * Total Train Length (locomotives and cars) in FEET
     */
    private int trainLength;

    /*
     * The tonnage that trails the locomotive consist. Trailing ton weights include
     * the equipment as well as the weight of the shipments. (from Loco Segment
     * 2032) in SHORT TONS
     */
    private int trailingTonnage;

    private String lastReportedTrackName;  // Most current Track reporting

    /*
     * The most current Milepost location of this train
     * such that 101.25 => 1/4 of the distance between MP 101 and 102
     */
    private float lastReportedMPLocation;

    /*
     * The train priority code that indicates one of the value as defined below.
     * 0 - Unknown
     * 1 - Premium (P, Z)
     * 2 - Priority (Q, K)
     * 3 - Special
     * 4 - Standard
     */
    private int priorityCode;

    /*
     * The train category code for categories such as Manifest, Auto etc.
     * 0 - Unknown
     * 1 - APL Double Stack
     * 2 - Auto
     * 3 - Beets Train
     * 4 - Coal
     * 5 - Ethanol
     * 6 - Foreign
     * 7 - Foreign Dispatch
     * 8 - Grain
     * 9 - Helper
     * 10 - Intermodal
     * 11 - Local
     * 12 - Manifest
     * 13 - Ore
     * 14 - Passenger
     * 15 - Power
     * 16 - Rock
     * 17 - Special
     * 18 - Unit
     * 19 - Work
     * 20 - Yard
     */

    private int categoryCode;

    /*
     * The train authority range that indicates one of the value as defined below.
     * 0 - Unknown
     * 1 - Local
     * 2 - Thru
     * 3 - Yard
     */
    private int authorityRangeCode;

    /*
     * The train ownership that indicates one of the value as defined below.
     * 0 - Unknown
     * 1 - Foreign
     * 2 - System
     * 3 - Trackage
     */
    private int ownershipCode;

    /*
     *  'The total trailing tons of cars and dead locomotives in short tons.'
     */
    private float actualTonsPerAxle;
    
}
