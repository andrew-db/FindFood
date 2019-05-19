package com.krakenjaws.findfood.modals;

public class PlacesDetails_Modal {

    /* Each cardView has one of these in the RecyclerView */
    public final String address;
    public final String phone_no;
    public final String distance;
    public final String name;
    public final String photourl;
    public final float rating;

    /**
     * The contents of our recycler view summary of Restaurant Location and Details.
     *
     * @param address  Restaurant address on a activity_map.
     * @param phone_no Restaurant's phone number.
     * @param rating   Restaurant rating like on YELP.
     * @param distance How far away the resturant is in kilometers. (1km = 0.6213712mi)
     * @param name     The Restaurant's name.
     * @param photurl  An image of the restaurant location and/or inside.
     */
    public PlacesDetails_Modal(String address, String phone_no, float rating, String distance, String name, String photurl) {
        this.address = address;
        this.phone_no = phone_no;
        this.rating = rating;
        this.distance = distance;
        this.name = name;
        this.photourl = photurl;
    }

}
