package com.krakenjaws.findfood.response;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

public class DistanceResponse {

    @SerializedName("status")
    public String status;

    @SerializedName("rows")
    public final ArrayList<InfoDistance> rows = new ArrayList<>();

    public class InfoDistance {
        @SerializedName("elements")
        public final ArrayList<DistanceElement> elements = new ArrayList<>();

        public class DistanceElement {
            @SerializedName("status")
            public String status;
            @SerializedName("duration")
            public ValueItem duration;
            @SerializedName("distance")
            public ValueItem distance;


        }

        public class ValueItem {
            @SerializedName("value")
            public long value;
            @SerializedName("text")
            public String text;

        }
    }


}
