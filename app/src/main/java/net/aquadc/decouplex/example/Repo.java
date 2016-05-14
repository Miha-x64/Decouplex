package net.aquadc.decouplex.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by miha on 14.05.16.
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Repo {

    public int id;

    public String name;

    @JsonProperty("full_name")
    public String fullName;

    public String description;

}
